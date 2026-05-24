package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.*;
import org.example.backend.dto.response.CustomerRoomResponseDTO;
import org.example.backend.dto.response.RoomResponseDTO;
import org.example.backend.entity.*;
import org.example.backend.entity.enums.RoomStatus;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomManagementService {

    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeImageRepository roomTypeImageRepository;
    private final HotelRepository hotelRepository;

    // ================= TỐI ƯU THUẬT TOÁN: THÊM PHÒNG / TÁI SINH PHÒNG =================
    @Transactional
    public Room createRoom(RoomRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách sạn ID: " + request.getHotelId()));

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng ID: " + request.getRoomTypeId()));

        // 1. Kiểm tra xem số phòng này có đang hoạt động công khai ngoài ma trận (is_deleted = false) không
        List<Room> activeRooms = roomRepository.findByHotelIdAndIsDeletedFalse(request.getHotelId());
        boolean isNumberActive = activeRooms.stream()
                .anyMatch(r -> r.getRoomNumber().equals(request.getRoomNumber()));

        if (isNumberActive) {
            throw new AppException("Số phòng " + request.getRoomNumber() + " đang hoạt động công khai trên sơ đồ lưới!", HttpStatus.BAD_REQUEST);
        }

        // 2. KỊCH BẢN TÁI SINH: Quét xem số phòng này có nằm trong kho lưu trữ ẨN (is_deleted = true) không
        Optional<Room> hiddenRoomOpt = roomRepository.findByHotelIdAndRoomNumberAndIsDeletedTrue(
                request.getHotelId(), request.getRoomNumber()
        );

        Room roomToSave;
        if (hiddenRoomOpt.isPresent()) {
            // Thực hiện tái sinh thực thể cũ để tiết kiệm dung lượng, sạch Database
            roomToSave = hiddenRoomOpt.get();
            roomToSave.setIsDeleted(false);
            roomToSave.setStatus(RoomStatus.AVAILABLE);
            roomToSave.setRoomType(roomType);
            roomToSave.setFloor(request.getFloor());
        } else {
            // 3. Nếu số phòng này mới tinh, tiến hành tạo dòng mới tự tăng ID bình thường
            roomToSave = new Room();
            roomToSave.setHotel(hotel);
            roomToSave.setRoomType(roomType);
            roomToSave.setRoomNumber(request.getRoomNumber());
            roomToSave.setFloor(request.getFloor());
            roomToSave.setStatus(RoomStatus.AVAILABLE);
            roomToSave.setIsDeleted(false);
        }

        Room savedRoom = roomRepository.save(roomToSave);

        // 4. Xử lý lưu album ảnh (Nếu tái sinh phòng cũ thì xóa album ảnh cũ đi để ghi đè album ảnh mới lên)
        roomImageRepository.deleteByRoomId(savedRoom.getId());

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<RoomImage> roomImagesToSave = new ArrayList<>();
            for (RoomImageRequest imgReq : request.getImages()) {
                if (imgReq.getImageUrl() != null && !imgReq.getImageUrl().trim().isEmpty()) {
                    RoomImage roomImg = new RoomImage();
                    roomImg.setRoom(savedRoom);
                    roomImg.setImageUrl(imgReq.getImageUrl());
                    roomImg.setIsPrimary(imgReq.getIsPrimary() != null ? imgReq.getIsPrimary() : false);
                    roomImagesToSave.add(roomImg);
                }
            }
            if (!roomImagesToSave.isEmpty()) {
                roomImageRepository.saveAll(roomImagesToSave);
            }
        }

        return savedRoom;
    }

    // ================= THIẾT LẬP PHÒNG HÀNG LOẠT (BATCH INSERT) =================
    @Transactional
    public void createBulkRooms(RoomBatchRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách sạn ID: " + request.getHotelId()));

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng ID: " + request.getRoomTypeId()));

        int currentRoomNum = request.getStartRoomNumber();
        List<RoomImage> allImagesToSave = new ArrayList<>();

        for (int i = 0; i < request.getTotalRooms(); i++) {
            String roomNumberStr = String.valueOf(currentRoomNum);

            // Kiểm tra trùng số phòng đang hoạt động
            List<Room> activeRooms = roomRepository.findByHotelIdAndIsDeletedFalse(request.getHotelId());
            boolean isNumberActive = activeRooms.stream().anyMatch(r -> r.getRoomNumber().equals(roomNumberStr));

            if (isNumberActive) {
                throw new AppException("Số phòng " + roomNumberStr + " đang hoạt động! Chuỗi khởi tạo hàng loạt bị dừng lại.", HttpStatus.BAD_REQUEST);
            }

            // Quét kho ẩn để thực hiện tái sinh nếu có dòng trùng lặp lịch sử
            Optional<Room> hiddenRoomOpt = roomRepository.findByHotelIdAndRoomNumberAndIsDeletedTrue(request.getHotelId(), roomNumberStr);
            Room roomToSave;

            if (hiddenRoomOpt.isPresent()) {
                roomToSave = hiddenRoomOpt.get();
                roomToSave.setIsDeleted(false);
                roomToSave.setStatus(RoomStatus.AVAILABLE);
                roomToSave.setRoomType(roomType);
                roomToSave.setFloor(request.getFloor());
            } else {
                roomToSave = new Room();
                roomToSave.setHotel(hotel);
                roomToSave.setRoomType(roomType);
                roomToSave.setRoomNumber(roomNumberStr);
                roomToSave.setFloor(request.getFloor());
                roomToSave.setStatus(RoomStatus.AVAILABLE);
                roomToSave.setIsDeleted(false);
            }

            Room savedRoom = roomRepository.save(roomToSave);

            // Xóa ảnh cũ nếu là phòng tái sinh trước khi map bộ ảnh mẫu hàng loạt
            roomImageRepository.deleteByRoomId(savedRoom.getId());

            if (request.getImages() != null && !request.getImages().isEmpty()) {
                for (RoomImageRequest imgReq : request.getImages()) {
                    RoomImage roomImg = new RoomImage();
                    roomImg.setRoom(savedRoom);
                    roomImg.setImageUrl(imgReq.getImageUrl());
                    roomImg.setIsPrimary(imgReq.getIsPrimary() != null ? imgReq.getIsPrimary() : false);
                    allImagesToSave.add(roomImg);
                }
            }
            currentRoomNum++;
        }

        if (!allImagesToSave.isEmpty()) {
            roomImageRepository.saveAll(allImagesToSave);
        }
    }

    // ================= CẬP NHẬT CHI TIẾT PHÒNG KHÁCH SẠN =================
    @Transactional
    public void updateRoomDetails(Long roomId, RoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ID: " + roomId));

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng ID: " + request.getRoomTypeId()));

        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setRoomType(roomType);
        roomRepository.save(room);

        if (request.getImages() != null) {
            roomImageRepository.deleteByRoomId(roomId);

            List<RoomImage> newImages = new ArrayList<>();
            for (RoomImageRequest imgReq : request.getImages()) {
                if (imgReq.getImageUrl() != null && !imgReq.getImageUrl().trim().isEmpty()) {
                    RoomImage roomImg = new RoomImage();
                    roomImg.setRoom(room);
                    roomImg.setImageUrl(imgReq.getImageUrl());
                    roomImg.setIsPrimary(imgReq.getIsPrimary() != null ? imgReq.getIsPrimary() : false);
                    newImages.add(roomImg);
                }
            }
            if (!newImages.isEmpty()) {
                roomImageRepository.saveAll(newImages);
            }
        }
    }

    // ================= TỐI ƯU NGHIỆP VỤ: XÓA MỀM PHÒNG (SOFT DELETE) =================
    @Transactional
    public void deleteRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ID: " + roomId));

        // CHUYỂN SANG XÓA MỀM: Đóng băng phòng để nuôi liên kết khóa ngoại với các hóa đơn đặt phòng cũ
        room.setIsDeleted(true);
        room.setStatus(RoomStatus.MAINTENANCE);
        roomRepository.save(room);
    }

    // ================= QUẢN LÝ LOẠI PHÒNG (ADMIN) =================
    @Transactional
    public RoomType createRoomType(RoomTypeRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách sạn ID: " + request.getHotelId()));

        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        roomType.setTypeName(request.getTypeName());
        roomType.setBasePrice(request.getBasePrice());
        roomType.setMaxOccupancy(request.getMaxOccupancy());
        roomType.setIsFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false);

        RoomType savedRoomType = roomTypeRepository.save(roomType);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<RoomTypeImage> imagesToSave = new ArrayList<>();
            for (RoomTypeImageRequest imgReq : request.getImages()) {
                RoomTypeImage img = new RoomTypeImage();
                img.setRoomType(savedRoomType);
                img.setImageUrl(imgReq.getImageUrl());
                img.setIsPrimary(imgReq.getIsPrimary() != null ? imgReq.getIsPrimary() : false);
                imagesToSave.add(img);
            }
            roomTypeImageRepository.saveAll(imagesToSave);
        }
        return savedRoomType;
    }

    @Transactional
    public void deleteRoomType(Long id) {
        if (!roomTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy loại phòng ID: " + id);
        }
        if (roomRepository.existsByRoomTypeId(id)) {
            throw new AppException("Không thể xóa loại phòng đang có phòng hoạt động ngoài ma trận!", HttpStatus.BAD_REQUEST);
        }
        roomTypeRepository.deleteById(id);
    }

    @Transactional
    public void updateRoomStatus(Long id, String status) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ID: " + id));
        try {
            room.setStatus(RoomStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new AppException("Trạng thái phòng không hợp lệ!", HttpStatus.BAD_REQUEST);
        }
        roomRepository.save(room);
    }

    // ================= LẤY SƠ ĐỒ MA TRẬN PHÒNG CHO ADMIN & STAFF (CHỈ HIỂN THỊ PHÒNG CHƯA XÓA MỀM) =================
    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getRoomsByHotel(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException("Không tìm thấy khách sạn ID: " + hotelId);
        }

        // CHỈ LẤY PHÒNG ĐANG HOẠT ĐỘNG (is_deleted = false)
        List<Room> rooms = roomRepository.findByHotelIdAndIsDeletedFalse(hotelId);

        return rooms.stream().map(room -> {
            List<String> album = room.getImages() != null ? room.getImages().stream()
                    .map(RoomImage::getImageUrl)
                    .collect(Collectors.toList()) : new ArrayList<>();

            String primaryUrl = "https://images.unsplash.com/photo-1566665797739-1674de7a421a?q=80&w=500";
            if (room.getImages() != null && !room.getImages().isEmpty()) {
                primaryUrl = room.getImages().stream()
                        .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                        .map(RoomImage::getImageUrl)
                        .findFirst().orElse(album.get(0));
            } else if (room.getRoomType() != null && room.getRoomType().getImages() != null && !room.getRoomType().getImages().isEmpty()) {
                primaryUrl = room.getRoomType().getImages().stream()
                        .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                        .map(RoomTypeImage::getImageUrl)
                        .findFirst().orElse(room.getRoomType().getImages().get(0).getImageUrl());
            }

            return RoomResponseDTO.builder()
                    .roomId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .floor(room.getFloor())
                    .status(room.getStatus() != null ? room.getStatus().name() : "AVAILABLE")
                    .typeName(room.getRoomType() != null ? room.getRoomType().getTypeName() : "N/A")
                    .price(room.getRoomType() != null ? room.getRoomType().getBasePrice() : java.math.BigDecimal.ZERO)
                    .hotelName(room.getHotel() != null ? room.getHotel().getName() : "N/A")
                    .imageUrl(primaryUrl)
                    .albumImages(album)
                    .build();
        }).collect(Collectors.toList());
    }

    // ================= LẤY DANH SÁCH PHÒNG CHO KHÁCH HÀNG (CUSTOMER APP) =================
    @Transactional(readOnly = true)
    public List<CustomerRoomResponseDTO> getRoomsForCustomer() {
        // CHỈ HIỂN THỊ PHÒNG CHƯA BỊ ẨN KHỎI HỆ THỐNG
        List<Room> rooms = roomRepository.findAll().stream()
                .filter(r -> r.getIsDeleted() != null && !r.getIsDeleted())
                .collect(Collectors.toList());

        return rooms.stream().map(room -> {
            CustomerRoomResponseDTO dto = new CustomerRoomResponseDTO();
            dto.setRoomId(room.getId());
            dto.setRoomNumber(room.getRoomNumber());
            dto.setTypeName(room.getRoomType() != null ? room.getRoomType().getTypeName() : "N/A");

            if (room.getRoomType() != null && room.getRoomType().getBasePrice() != null) {
                dto.setPrice(room.getRoomType().getBasePrice().doubleValue());
            } else {
                dto.setPrice(0.0);
            }

            dto.setDescription(room.getRoomType() != null ? room.getRoomType().getTypeName() + " sang trọng đầy đủ tiện nghi, mang lại trải nghiệm nghỉ dưỡng tuyệt vời." : "");

            String mainImg = "";
            if (room.getImages() != null && !room.getImages().isEmpty()) {
                mainImg = room.getImages().stream()
                        .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                        .map(RoomImage::getImageUrl)
                        .findFirst().orElse(room.getImages().get(0).getImageUrl());
            } else if (room.getRoomType() != null && room.getRoomType().getImages() != null && !room.getRoomType().getImages().isEmpty()) {
                mainImg = room.getRoomType().getImages().stream()
                        .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                        .map(RoomTypeImage::getImageUrl)
                        .findFirst().orElse(room.getRoomType().getImages().get(0).getImageUrl());
            }
            dto.setMainImageUrl(mainImg);

            List<String> album = room.getImages() != null ? room.getImages().stream()
                    .map(RoomImage::getImageUrl)
                    .collect(Collectors.toList()) : new ArrayList<>();

            if (album.isEmpty() && room.getRoomType() != null && room.getRoomType().getImages() != null) {
                album = room.getRoomType().getImages().stream()
                        .map(RoomTypeImage::getImageUrl)
                        .collect(Collectors.toList());
            }
            dto.setAlbumImages(album);

            return dto;
        }).collect(Collectors.toList());
    }

    // Hàm DTO Helper phụ trợ
    private RoomResponseDTO convertToDTO(Room room) {
        return RoomResponseDTO.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .status(room.getStatus() != null ? room.getStatus().name() : "AVAILABLE")
                .typeName(room.getRoomType() != null ? room.getRoomType().getTypeName() : "N/A")
                .price(room.getRoomType() != null ? room.getRoomType().getBasePrice() : java.math.BigDecimal.ZERO)
                .build();
    }
    @Transactional(readOnly = true)
    public List<RoomType> getRoomTypes() {
        // Gọi xuống roomTypeRepository để lấy toàn bộ danh sách phân loại phòng trong DB
        return roomTypeRepository.findAll();
    }
}