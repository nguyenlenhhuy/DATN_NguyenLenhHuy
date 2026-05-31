package org.example.backend.service;

import jakarta.persistence.criteria.*;
import org.example.backend.dto.response.RoomResponseDTO;
import org.example.backend.dto.request.RoomSearchRequest;
import org.example.backend.dto.response.RoomTypeDetailResponse;
import org.example.backend.entity.BookingDetail;
import org.example.backend.entity.Room;
import org.example.backend.entity.RoomType;
import org.example.backend.entity.RoomImage;
import org.example.backend.repository.RoomRepository;
import org.example.backend.repository.RoomTypeRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    public RoomService(RoomRepository roomRepository, RoomTypeRepository roomTypeRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    public RoomResponseDTO getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin phòng với mã ID: " + id));

        if (room.isDeleted()) {
            throw new RuntimeException("Phòng này không tồn tại.");
        }

        return this.mapToDTO(room);
    }

    public List<RoomResponseDTO> searchRooms(RoomSearchRequest request) {
        Specification<Room> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (request.getCheckIn() != null && request.getCheckOut() != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<BookingDetail> bDetail = subquery.from(BookingDetail.class);
                subquery.select(bDetail.get("room").get("id"));

                Predicate overlap = cb.and(
                        cb.lessThan(bDetail.get("booking").get("checkInDate"), request.getCheckOut()),
                        cb.greaterThan(bDetail.get("booking").get("checkOutDate"), request.getCheckIn())
                );
                subquery.where(overlap);
                predicates.add(cb.not(root.get("id").in(subquery)));
            }

            if (request.getGuestCount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("roomType").get("maxOccupancy"), request.getGuestCount()));
            }

            if (request.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("roomType").get("basePrice"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("roomType").get("basePrice"), request.getMaxPrice()));
            }

            if (request.getTypeName() != null && !request.getTypeName().isEmpty()) {
                predicates.add(cb.equal(root.get("roomType").get("typeName"), request.getTypeName()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return roomRepository.findAll(spec).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<RoomResponseDTO> getFeaturedRooms() {
        return roomRepository.findFeaturedRooms().stream()
                .filter(room -> !room.isDeleted())
                .limit(6)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<RoomResponseDTO> getAllRooms() {
        return roomRepository.findAll().stream()
                .filter(room -> !room.isDeleted())
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private RoomResponseDTO mapToDTO(Room room) {
        String thumbnail = null;
        List<String> allImages = new ArrayList<>();

        if (room.getImages() != null && !room.getImages().isEmpty()) {
            // Lấy danh sách tất cả URL để làm album ảnh
            allImages = room.getImages().stream()
                    .map(RoomImage::getImageUrl)
                    .collect(Collectors.toList());

            // Ưu tiên lấy ảnh có isPrimary = true làm ảnh đại diện (thumbnail)
            thumbnail = room.getImages().stream()
                    .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                    .map(RoomImage::getImageUrl)
                    .findFirst()
                    .orElse(allImages.get(0)); // Fallback: Nếu không có ảnh primary, lấy ảnh đầu tiên
        }

        String currentStatus = room.getStatus() != null ? room.getStatus().name() : "AVAILABLE";

        return RoomResponseDTO.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .status(currentStatus)
                .isDeleted(room.isDeleted())
                .typeName(room.getRoomType() != null ? room.getRoomType().getTypeName() : "N/A")
                .price(room.getPrice() != null
                        ? BigDecimal.valueOf(room.getPrice())
                        : (room.getRoomType() != null && room.getRoomType().getBasePrice() != null
                        ? room.getRoomType().getBasePrice()
                        : BigDecimal.ZERO))
                .hotelName(room.getHotel() != null ? room.getHotel().getName() : "N/A")
                .imageUrl(thumbnail)
                .albumImages(allImages)
                .build();
    }

    public List<RoomTypeDetailResponse> getAllRoomTypeDTOs() {
        return roomTypeRepository.findAll().stream()
                .map(this::mapToRoomTypeDetailResponse)
                .collect(Collectors.toList());
    }

    private RoomTypeDetailResponse mapToRoomTypeDetailResponse(RoomType roomType) {
        return RoomTypeDetailResponse.builder()
                .id(roomType.getId())
                .typeName(roomType.getTypeName())
                .basePrice(roomType.getBasePrice())
                .maxOccupancy(roomType.getMaxOccupancy())
                .hotelName(roomType.getHotel() != null ? roomType.getHotel().getName() : "N/A")
                .address(roomType.getHotel() != null ? roomType.getHotel().getAddress() : "N/A")
                .imageUrls(roomType.getImageUrls() != null ? roomType.getImageUrls() : Collections.emptyList())
                .build();
    }
}