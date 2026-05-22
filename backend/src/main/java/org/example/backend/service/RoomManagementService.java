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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomManagementService {

    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeImageRepository roomTypeImageRepository;
    private final HotelRepository hotelRepository;

    // ================= LOGIC THÊM PHÒNG + BỘ ẢNH RIÊNG BIỆT (CÁCH 2) =================
    @Transactional
    public Room createRoom(RoomRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách sạn ID: " + request.getHotelId()));

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng ID: " + request.getRoomTypeId()));

        if (roomRepository.existsByHotelIdAndRoomNumber(request.getHotelId(), request.getRoomNumber())) {
            throw new AppException("Số phòng " + request.getRoomNumber() + " đã tồn tại trong khách sạn này!", HttpStatus.BAD_REQUEST);
        }

        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomType(roomType);
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setStatus(RoomStatus.AVAILABLE);

        Room savedRoom = roomRepository.save(room);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<RoomImage> roomImagesToSave = new ArrayList<>();
            for (RoomImageRequest imgReq : request.getImages()) {
                RoomImage roomImg = new RoomImage();
                roomImg.setRoom(savedRoom); // SỬA LỖI: Gán trực tiếp đối tượng đã sinh ID
                roomImg.setImageUrl(imgReq.getImageUrl());
                roomImg.setIsPrimary(imgReq.getIsPrimary() != null ? imgReq.getIsPrimary() : false);
                roomImagesToSave.add(roomImg);
            }
            roomImageRepository.saveAll(roomImagesToSave);
        }

        return savedRoom;
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
                img.setImageUrl(imgReq.getImageUrl()); // ĐÚNG: Lấy từ imgReq thông qua GET, nạp vào img thông qua SET
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
            throw new AppException("Không thể xóa loại phòng đang có phòng hoạt động!", HttpStatus.BAD_REQUEST);
        }
        roomTypeRepository.deleteById(id);
    }

    // ================= XÓA & ĐỔI TRẠNG THÁI PHÒNG =================
    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ID: " + id));

        if (!RoomStatus.AVAILABLE.equals(room.getStatus())) {
            throw new AppException("Không thể xóa phòng khi đang ở trạng thái: " + room.getStatus(), HttpStatus.BAD_REQUEST);
        }
        roomRepository.deleteById(id);
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

    // ================= LẤY SƠ ĐỒ PHÒNG CHO ADMIN & STAFF (ĐỒNG BỘ ANGULAR) =================
    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getRoomsByHotel(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException("Không tìm thấy khách sạn ID: " + hotelId);
        }

        List<Room> rooms = roomRepository.findByHotelId(hotelId);

        return rooms.stream().map(room -> {
            // SỬA LỖI LOGIC: Lấy danh sách chuỗi album ảnh để truyền về cho Angular Modal nạp dữ liệu
            List<String> album = room.getImages() != null ? room.getImages().stream()
                    .map(RoomImage::getImageUrl)
                    .collect(Collectors.toList()) : new ArrayList<>();

            // Thuật toán lấy ảnh đại diện (Primary) của phòng, nếu trống thì fallback lấy ảnh Loại phòng
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
                    .imageUrl(primaryUrl)     // Thêm trường đồng bộ hóa với Angular Card [src]
                    .albumImages(album)       // Thêm mảng album ảnh đồng bộ với Angular Modal [ngFor]
                    .build();
        }).collect(Collectors.toList());
    }

    // ================= LẤY DANH SÁCH PHÒNG HIỂN THỊ TRANG CHỦ (CUSTOMER APP) =================
    @Transactional(readOnly = true)
    public List<CustomerRoomResponseDTO> getRoomsForCustomer() {
        List<Room> rooms = roomRepository.findAll();

        return rooms.stream().map(room -> {
            CustomerRoomResponseDTO dto = new CustomerRoomResponseDTO();
            dto.setRoomId(room.getId());
            dto.setRoomNumber(room.getRoomNumber());
            dto.setTypeName(room.getRoomType().getTypeName());

            // Chuyển đổi an toàn từ BigDecimal sang Double ngày thường
            if (room.getRoomType().getBasePrice() != null) {
                dto.setPrice(room.getRoomType().getBasePrice().doubleValue());
            } else {
                dto.setPrice(0.0);
            }

            dto.setDescription(room.getRoomType().getTypeName() + " sang trọng đầy đủ tiện nghi, mang lại trải nghiệm nghỉ dưỡng tuyệt vời.");

            // Trích xuất ảnh chính (Primary) cho phía Customer
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

            // Gộp album ảnh
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
    // Thêm hàm này vào trong file RoomManagementService.java

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

            // Kiểm tra trùng số phòng
            if (roomRepository.existsByHotelIdAndRoomNumber(request.getHotelId(), roomNumberStr)) {
                throw new AppException("Số phòng " + roomNumberStr + " đã tồn tại trong hệ thống! Chuỗi khởi tạo hàng loạt bị dừng lại.", HttpStatus.BAD_REQUEST);
            }

            // 1. Tạo thực thể phòng
            Room room = new Room();
            room.setHotel(hotel);
            room.setRoomType(roomType);
            room.setRoomNumber(roomNumberStr);
            room.setFloor(request.getFloor());
            room.setStatus(RoomStatus.AVAILABLE);

            Room savedRoom = roomRepository.save(room);

            // 2. Nhân bản bộ ảnh chung gán cho phòng này
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                for (RoomImageRequest imgReq : request.getImages()) {
                    RoomImage roomImg = new RoomImage();
                    roomImg.setRoom(savedRoom);
                    roomImg.setImageUrl(imgReq.getImageUrl()); // Sử dụng hàm GET chuẩn xác
                    roomImg.setIsPrimary(imgReq.getIsPrimary() != null ? imgReq.getIsPrimary() : false);
                    allImagesToSave.add(roomImg);
                }
            }
            currentRoomNum++;
        }

        // Lưu toàn bộ ảnh của cả loạt phòng xuống DB trong 1 câu lệnh batch-insert
        if (!allImagesToSave.isEmpty()) {
            roomImageRepository.saveAll(allImagesToSave);
        }
    }
    @Transactional
    public void updateRoomDetails(Long roomId, RoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ID: " + roomId));

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng ID: " + request.getRoomTypeId()));

        // 1. Cập nhật các thông tin cơ bản
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setRoomType(roomType);
        roomRepository.save(room);

        // 2. Cập nhật bộ sưu tập ảnh mới (Xóa album ảnh cũ của phòng này đi và ghi đè album mới lên)
        if (request.getImages() != null) {
            // Xóa sạch liên kết ảnh cũ của riêng phòng này trong DB
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
}