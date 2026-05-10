package org.example.backend.service;

import jakarta.persistence.criteria.*;
import org.example.backend.dto.response.RoomResponseDTO;
import org.example.backend.dto.request.RoomSearchRequest;
import org.example.backend.entity.BookingDetail;
import org.example.backend.entity.Room;
import org.example.backend.repository.RoomRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<RoomResponseDTO> searchRooms(RoomSearchRequest request) {
        Specification<Room> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc phòng trống: Loại bỏ các phòng đã nằm trong BookingDetail có ngày trùng lặp
            if (request.getCheckIn() != null && request.getCheckOut() != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<BookingDetail> bDetail = subquery.from(BookingDetail.class);
                subquery.select(bDetail.get("room").get("id"));

                // Logic: booking_start < search_end AND booking_end > search_start
                Predicate overlap = cb.and(
                        cb.lessThan(bDetail.get("booking").get("checkInDate"), request.getCheckOut()),
                        cb.greaterThan(bDetail.get("booking").get("checkOutDate"), request.getCheckIn())
                );
                subquery.where(overlap);
                predicates.add(cb.not(root.get("id").in(subquery)));
            }

            // 2. Sức chứa
            if (request.getGuestCount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("roomType").get("maxOccupancy"), request.getGuestCount()));
            }

            // 3. Khoảng giá
            if (request.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("roomType").get("basePrice"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("roomType").get("basePrice"), request.getMaxPrice()));
            }

            // 4. Loại phòng
            if (request.getTypeName() != null && !request.getTypeName().isEmpty()) {
                predicates.add(cb.equal(root.get("roomType").get("typeName"), request.getTypeName()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return roomRepository.findAll(spec).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<RoomResponseDTO> getFeaturedRooms() {
        // Lấy tối đa 6 phòng ưu tiên cho trang chủ
        return roomRepository.findFeaturedRooms().stream()
                .limit(6)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private RoomResponseDTO mapToDTO(Room room) {
        return RoomResponseDTO.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .typeName(room.getRoomType().getTypeName())
                .price(room.getRoomType().getBasePrice())
                .hotelName(room.getHotel().getName())
                .build();
    }
}