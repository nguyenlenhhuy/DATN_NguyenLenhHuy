package org.example.backend.service;

import jakarta.persistence.criteria.*;
import org.example.backend.dto.response.BookedDateRangeDTO;
import org.example.backend.dto.response.RoomResponseDTO;
import org.example.backend.dto.request.RoomSearchRequest;
import org.example.backend.dto.response.RoomTypeDetailResponse;
import org.example.backend.entity.BookingDetail;
import org.example.backend.entity.Room;
import org.example.backend.entity.RoomType;
import org.example.backend.entity.RoomImage;
import org.example.backend.repository.BookingRepository;
import org.example.backend.repository.ReviewRepository;
import org.example.backend.repository.RoomRepository;
import org.example.backend.repository.RoomTypeRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository, RoomTypeRepository roomTypeRepository,
                       ReviewRepository reviewRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
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

            // Tránh trùng lặp Join bằng cách khai báo rõ ràng (Explicit Join)
            Join<Room, RoomType> roomTypeJoin = root.join("roomType", JoinType.INNER);

            // 1. Chỉ lấy phòng chưa bị xóa
            predicates.add(cb.equal(root.get("isDeleted"), false));
            // BUG FIX: Khi có khoảng ngày → chỉ loại phòng MAINTENANCE (đang sửa chữa, không thể đặt);
            // trạng thái vận hành hiện tại (OCCUPIED/DIRTY/RESERVED) không cản khách đặt tương lai.
            // Subquery bên dưới đã kiểm tra trùng lịch booking thực tế.
            // Khi KHÔNG có ngày → chỉ hiển thị phòng đang AVAILABLE (duyệt thông thường).
            if (request.getCheckIn() != null && request.getCheckOut() != null) {
                predicates.add(cb.notEqual(root.get("status"), org.example.backend.entity.enums.RoomStatus.MAINTENANCE));
            } else {
                predicates.add(cb.equal(root.get("status"), org.example.backend.entity.enums.RoomStatus.AVAILABLE));
            }

            // 2. Xử lý khoảng thời gian (Fix lỗi dính Booking đã hủy)
            if (request.getCheckIn() != null && request.getCheckOut() != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<BookingDetail> bDetail = subquery.from(BookingDetail.class);
                subquery.select(bDetail.get("room").get("id"));

                // Giả sử Booking có trường trạng thái là 'status' (bạn thay đổi cho khớp entity)
                Predicate overlap = cb.and(
                        cb.lessThan(bDetail.get("booking").get("checkInDate"), request.getCheckOut()),
                        cb.greaterThan(bDetail.get("booking").get("checkOutDate"), request.getCheckIn()),
                        // CHỈ lọc những booking đang còn hiệu lực (chưa bị hủy)
                        cb.notEqual(bDetail.get("booking").get("status"), "CANCELLED")
                );
                subquery.where(overlap);
                predicates.add(cb.not(root.get("id").in(subquery)));
            }

            // 3. Xử lý sức chứa (Chỉ áp dụng nếu user muốn tìm ĐÚNG 1 phòng chứa đủ n người)
            if (request.getGuestCount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(roomTypeJoin.get("maxOccupancy"), request.getGuestCount()));
            }

            // 4. Xử lý giá cả (Sử dụng Join đã khai báo)
            if (request.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(roomTypeJoin.get("basePrice"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(roomTypeJoin.get("basePrice"), request.getMaxPrice()));
            }

            // 5. Xử lý loại phòng
            if (request.getTypeName() != null && !request.getTypeName().trim().isEmpty()) {
                predicates.add(cb.equal(roomTypeJoin.get("typeName"), request.getTypeName().trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return roomRepository.findAll(spec).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<RoomResponseDTO> getFeaturedRooms() {
        // 1. Lấy danh sách phòng cấu hình nổi bật từ Repository
        List<Room> featuredRooms = roomRepository.findFeaturedRooms();

        // 2. CRITICAL FALLBACK: Nếu DB chưa có phòng nổi bật, tự động bốc phòng trống (AVAILABLE) để cứu giao diện
        if (featuredRooms.isEmpty()) {
            featuredRooms = roomRepository.findFeaturedRoomsFallback();
        }

        // 3. Xử lý Stream chuẩn: LỌC những phòng chưa xóa TRƯỚC, rồi mới GIỚI HẠN số lượng hiển thị
        return featuredRooms.stream()
                .filter(room -> room != null && !room.isDeleted()) // Lọc an toàn
                .filter(room -> room.getStatus() == org.example.backend.entity.enums.RoomStatus.AVAILABLE) // Chỉ lấy phòng đang trống
                .map(this::mapToDTO) // Chuyển đổi sang DTO
                .limit(4) // Giới hạn 4 phòng để hiển thị vừa vặn với Grid 4 cột của Tailwind CSS bạn đã thiết kế
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
            allImages = room.getImages().stream()
                    .map(RoomImage::getImageUrl)
                    .collect(Collectors.toList());

            thumbnail = room.getImages().stream()
                    .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                    .map(RoomImage::getImageUrl)
                    .findFirst()
                    .orElse(allImages.get(0));
        }

        String currentStatus = room.getStatus() != null ? room.getStatus().name() : "AVAILABLE";

        // 🔥 THÊM MỚI LOGIC: Tự động lọc ra các mã giảm giá đang kích hoạt và còn hạn dùng
        List<org.example.backend.dto.response.PromotionResponseDTO> appliedPromotions = new ArrayList<>();
        if (room.getPromotions() != null && !room.getPromotions().isEmpty()) {
            appliedPromotions = room.getPromotions().stream()
                    .filter(p -> p.getIsActive() != null && p.getIsActive()) // Mã phải đang bật
                    .filter(p -> p.getEndDate() == null || !p.getEndDate().isBefore(java.time.LocalDate.now())) // Mã còn hạn dùng
                    .map(this::mapPromotionToDTO) // Chuyển đổi sang DTO
                    .collect(Collectors.toList());
        }

        Double avgRating = null;
        Long revCount = 0L;
        if (room.getHotel() != null) {
            try {
                Object[] stats = reviewRepository.getRatingStats(room.getHotel().getId());
                if (stats != null && stats.length >= 2) {
                    if (stats[0] != null) revCount = ((Number) stats[0]).longValue();
                    if (stats[1] != null) avgRating = ((Number) stats[1]).doubleValue();
                }
            } catch (Exception ignored) {}
        }

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
                .appliedPromotions(appliedPromotions)
                .maxGuests(room.getRoomType() != null ? room.getRoomType().getMaxOccupancy() : null)
                .hotelAddress(room.getHotel() != null ? room.getHotel().getAddress() : null)
                .hotelStarRating(room.getHotel() != null ? room.getHotel().getStarRating() : null)
                .averageRating(avgRating)
                .reviewCount(revCount)
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
    private org.example.backend.dto.response.PromotionResponseDTO mapPromotionToDTO(org.example.backend.entity.Promotion promotion) {
        if (promotion == null) return null;
        return org.example.backend.dto.response.PromotionResponseDTO.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .discountPercentage(promotion.getDiscountPercentage())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .isActive(promotion.getIsActive())
                .build();
    }

    /**
     * Trả về danh sách khoảng ngày đã đặt (pending/confirmed/check-in) của một phòng.
     * Frontend dùng để vẽ lịch phòng và cảnh báo trùng ngày trước khi đặt.
     */
    public List<BookedDateRangeDTO> getBookedRanges(Long roomId, LocalDate from, LocalDate to) {
        return bookingRepository.findBookedRangesForRoom(roomId, from, to)
                .stream()
                .map(row -> new BookedDateRangeDTO((LocalDate) row[0], (LocalDate) row[1]))
                .collect(Collectors.toList());
    }

}