package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.*;
import org.example.backend.dto.response.*;
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

    // ✔️ INJECT THÊM REPOSITORY ƯU ĐÃI ĐỂ PHỤC VỤ LUỒNG NGHIỆP VỤ
    private final PromotionRepository promotionRepository;

    @Transactional(readOnly = true)
    public List<RoomType> getRoomTypes() {
        return roomTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RoomTypeDetailResponse> getAllRoomTypeDTOs() {
        return roomTypeRepository.findAll().stream().map(type -> {
            List<String> images = type.getImages() != null ?
                    type.getImages().stream().map(RoomTypeImage::getImageUrl).collect(Collectors.toList()) :
                    new ArrayList<>();

            return RoomTypeDetailResponse.builder()
                    .id(type.getId())
                    .typeName(type.getTypeName())
                    .basePrice(type.getBasePrice())
                    .maxOccupancy(type.getMaxOccupancy())
                    .hotelName(type.getHotel() != null ? type.getHotel().getName() : "N/A")
                    .imageUrls(images)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public Room createRoom(RoomRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách sạn ID: " + request.getHotelId()));

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng ID: " + request.getRoomTypeId()));

        List<Room> activeRooms = roomRepository.findByHotelIdAndIsDeletedFalse(request.getHotelId());
        boolean isNumberActive = activeRooms.stream().anyMatch(r -> r.getRoomNumber().equals(request.getRoomNumber()));

        if (isNumberActive) {
            throw new AppException("Số phòng " + request.getRoomNumber() + " đang hoạt động công khai trên sơ đồ lưới!", HttpStatus.BAD_REQUEST);
        }

        Optional<Room> hiddenRoomOpt = roomRepository.findByHotelIdAndRoomNumberAndIsDeletedTrue(request.getHotelId(), request.getRoomNumber());

        Room roomToSave;
        boolean isBrandNewRoom = false;

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
            roomToSave.setRoomNumber(request.getRoomNumber());
            roomToSave.setFloor(request.getFloor());
            roomToSave.setStatus(RoomStatus.AVAILABLE);
            roomToSave.setIsDeleted(false);
            isBrandNewRoom = true;
        }

        Room savedRoom = roomRepository.save(roomToSave);

        if (!isBrandNewRoom) {
            roomImageRepository.deleteByRoomId(savedRoom.getId());
        }

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
            List<Room> activeRooms = roomRepository.findByHotelIdAndIsDeletedFalse(request.getHotelId());
            if (activeRooms.stream().anyMatch(r -> r.getRoomNumber().equals(roomNumberStr))) {
                throw new AppException("Số phòng " + roomNumberStr + " đang hoạt động!", HttpStatus.BAD_REQUEST);
            }

            Optional<Room> hiddenRoomOpt = roomRepository.findByHotelIdAndRoomNumberAndIsDeletedTrue(request.getHotelId(), roomNumberStr);
            Room roomToSave;
            boolean isBrandNewBatchRoom = false;

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
                isBrandNewBatchRoom = true;
            }

            Room savedRoom = roomRepository.save(roomToSave);

            if (!isBrandNewBatchRoom) {
                roomImageRepository.deleteByRoomId(savedRoom.getId());
            }

            if (request.getImages() != null && !request.getImages().isEmpty()) {
                for (RoomImageRequest imgReq : request.getImages()) {
                    if (imgReq.getImageUrl() != null && !imgReq.getImageUrl().trim().isEmpty()) {
                        RoomImage roomImg = new RoomImage();
                        roomImg.setRoom(savedRoom);
                        roomImg.setImageUrl(imgReq.getImageUrl());
                        roomImg.setIsPrimary(imgReq.getIsPrimary() != null ? imgReq.getIsPrimary() : false);
                        allImagesToSave.add(roomImg);
                    }
                }
            }
            currentRoomNum++;
        }
        if (!allImagesToSave.isEmpty()) {
            roomImageRepository.saveAll(allImagesToSave);
        }
    }

    @Transactional
    public void updateRoomDetails(Long roomId, RoomRequest request) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ID: " + roomId));
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng ID: " + request.getRoomTypeId()));

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

    @Transactional
    public void deleteRoom(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ID: " + roomId));
        room.setIsDeleted(true);
        room.setStatus(RoomStatus.MAINTENANCE);
        roomRepository.save(room);
    }

    @Transactional
    public RoomType createRoomType(RoomTypeRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách sạn ID: " + request.getHotelId()));
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
        if (!roomTypeRepository.existsById(id)) throw new ResourceNotFoundException("Không tìm thấy loại phòng ID: " + id);
        if (roomRepository.existsByRoomTypeId(id)) throw new AppException("Không thể xóa loại phòng đang có phòng hoạt động!", HttpStatus.BAD_REQUEST);
        roomTypeRepository.deleteById(id);
    }

    @Transactional
    public void updateRoomStatus(Long id, String status) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ID: " + id));
        try {
            room.setStatus(RoomStatus.valueOf(status.toUpperCase()));
        } catch (Exception e) {
            throw new AppException("Trạng thái không hợp lệ!", HttpStatus.BAD_REQUEST);
        }
        roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getRoomsByHotel(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) throw new ResourceNotFoundException("Không tìm thấy khách sạn ID: " + hotelId);
        return roomRepository.findByHotelIdAndIsDeletedFalse(hotelId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomerRoomResponseDTO> getRoomsForCustomer() {
        return roomRepository.findAll().stream().filter(r -> r.getIsDeleted() != null && !r.getIsDeleted()).map(room -> {
            CustomerRoomResponseDTO dto = new CustomerRoomResponseDTO();
            dto.setRoomId(room.getId());
            dto.setRoomNumber(room.getRoomNumber());
            dto.setTypeName(room.getRoomType() != null ? room.getRoomType().getTypeName() : "N/A");
            dto.setPrice(room.getRoomType() != null && room.getRoomType().getBasePrice() != null ? room.getRoomType().getBasePrice().doubleValue() : 0.0);
            return dto;
        }).collect(Collectors.toList());
    }

    // ================= 🔥 HÀM XỬ LÝ CHUẨN HÓA LƯU KHUYẾN MÃI VÀO COLUMN APPLIED_PROMOTION_ID =================
    @Transactional
    public void applyPromotionToRoom(Long roomId, Long promotionId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ID: " + roomId));

        if (promotionId == null || promotionId <= 0) {
            room.setAppliedPromotion(null);
        } else {
            Promotion promotion = promotionRepository.findById(promotionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình ưu đãi ID: " + promotionId));
            room.setAppliedPromotion(promotion);
        }
        roomRepository.save(room);
    }

    private RoomResponseDTO convertToDTO(Room room) {
        List<String> album = room.getImages() != null ? room.getImages().stream()
                .map(RoomImage::getImageUrl).collect(Collectors.toList()) : new ArrayList<>();

        String primaryUrl = "https://images.unsplash.com/photo-1566665797739-1674de7a421a?q=80&w=500";
        if (room.getImages() != null && !room.getImages().isEmpty()) {
            primaryUrl = room.getImages().stream()
                    .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                    .map(RoomImage::getImageUrl).findFirst().orElse(album.get(0));
        }

        // ✔️ ĐỒNG BỘ DỮ LIỆU ĐỔ RA CLIENT: Đọc mã giảm giá đang kích hoạt từ quan hệ appliedPromotion
        List<PromotionResponseDTO> promoDTOs = new ArrayList<>();
        if (room.getAppliedPromotion() != null) {
            Promotion p = room.getAppliedPromotion();
            promoDTOs.add(PromotionResponseDTO.builder()
                    .id(p.getId())
                    .code(p.getCode())
                    .discountPercentage(p.getDiscountPercentage())
                    .endDate(p.getEndDate())
                    .build());
        }

        return RoomResponseDTO.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .status(room.getStatus() != null ? room.getStatus().name() : "AVAILABLE")
                .typeName(room.getRoomType() != null ? room.getRoomType().getTypeName() : "N/A")
                .price(room.getRoomType() != null ? room.getRoomType().getBasePrice() : java.math.BigDecimal.ZERO)
                .imageUrl(primaryUrl)
                .albumImages(album)
                .appliedPromotions(promoDTOs) // Trả về dạng mảng khớp hoàn toàn với DTO và UI Angular cũ
                .build();
    }
}