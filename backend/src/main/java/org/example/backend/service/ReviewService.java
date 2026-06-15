package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ReplyRequestDTO;
import org.example.backend.dto.request.ReviewRequestDTO;
import org.example.backend.dto.response.ReviewResponseDTO;
import org.example.backend.entity.*;
import org.example.backend.entity.enums.BookingStatus;
import org.example.backend.entity.enums.MediaType;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.exception.ReviewAlreadyExistsException;
import org.example.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final AuditLogRepository auditLogRepository;
    private final RoomRepository roomRepository;

    // ================= DÀNH CHO KHÁCH HÀNG THAM QUAN PHÒNG =================

    /**
     * 🔥 THÊM MỚI: Lấy danh sách đánh giá thật của phòng từ Database MySQL
     * Đưa qua hàm mapToDTO để bứt gãy lỗi vòng lặp tuần hoàn JSON (Infinite Recursion Error)
     */
    public List<ReviewResponseDTO> getReviewsByRoomId(Long roomId) {
        // Gọi xuống câu lệnh SQL vừa cập nhật ở Bước 1
        List<Review> reviews = reviewRepository.findByBookingRoomIdOrderByCreatedAtDesc(roomId);

        // Trả về mảng rỗng an toàn nếu không tìm thấy bản ghi nào dưới MySQL
        if (reviews == null || reviews.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return reviews.stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ================= DÀNH CHO KHÁCH HÀNG ĐẶT PHÒNG =================

    @Transactional
    public ReviewResponseDTO submitReview(Long userId, ReviewRequestDTO dto) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (booking.getUser() == null) {
            throw new IllegalStateException("Đơn đặt phòng này bị lỗi dữ liệu (không có thông tin khách hàng). Vui lòng kiểm tra lại Database.");
        }

        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền đánh giá đơn hàng này");
        }

        if (booking.getStatus() == null || !booking.getStatus().name().equals("CHECK_OUT")) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá sau khi đã hoàn tất thủ tục trả phòng (Check-out).");
        }

        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng cần đánh giá"));

        if (reviewRepository.existsByBookingIdAndRoomId(dto.getBookingId(), dto.getRoomId())) {
            throw new ReviewAlreadyExistsException("Phòng này trong đơn hàng đã được đánh giá");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setRoom(room);
        review.setUser(booking.getUser());
        review.setHotel(booking.getHotel());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setCreatedAt(LocalDateTime.now());

        if (dto.getMediaUrls() != null) {
            List<ReviewMedia> mediaList = dto.getMediaUrls().stream()
                    .map(url -> new ReviewMedia(null, review, url,
                            url.toLowerCase().endsWith(".mp4") ? MediaType.VIDEO : MediaType.IMAGE))
                    .toList();
            review.setMediaList(mediaList);
        }

        Review saved = reviewRepository.save(review);
        updateHotelStats(booking.getHotel().getId());
        return mapToDTO(saved);
    }

    // ================= DÀNH CHO ADMIN / STAFF =================

    @Transactional
    public ReviewResponseDTO replyReview(Long reviewId, ReplyRequestDTO replyDTO) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá để phản hồi"));

        review.setReplyContent(replyDTO.getReplyContent());
        Review saved = reviewRepository.save(review);
        return mapToDTO(saved);
    }

    @Transactional
    public void deleteReviewByAdmin(Long reviewId, Long adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại"));

        Long hotelId = review.getHotel().getId();
        String description = "Admin xóa đánh giá ID: " + reviewId + " của khách " + review.getUser().getFullName();

        AuditLog log = new AuditLog(adminId, "DELETE_REVIEW", reviewId, description);
        auditLogRepository.save(log);

        reviewRepository.delete(review);
        updateHotelStats(hotelId);
    }

    public Page<ReviewResponseDTO> findAllForAdmin(int page, int size) {
        return reviewRepository.findAll(PageRequest.of(page, size))
                .map(this::mapToDTO);
    }

    // ================= HÀM HỖ TRỢ ĐỒNG BỘ MATRIX =================

    private void updateHotelStats(Long hotelId) {
        Object[] stats = (Object[]) reviewRepository.getRatingStats(hotelId);
        if (stats != null && stats.length > 0) {
            Object[] data = (Object[]) stats[0];
            Long total = (Long) data[0];
            Double avg = (Double) data[1];

            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khách sạn không tồn tại"));

            hotel.setTotalReviews(total != null ? total.intValue() : 0);
            hotel.setAvgRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
            hotelRepository.save(hotel);
        }
    }

    private ReviewResponseDTO mapToDTO(Review review) {
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .userName(review.getUser() != null ? review.getUser().getFullName() : "Khách ẩn danh")
                .roomNumber(review.getRoom() != null ? review.getRoom().getRoomNumber() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .replyContent(review.getReplyContent())
                .createdAt(review.getCreatedAt())
                .mediaUrls(review.getMediaList() != null ?
                        review.getMediaList().stream().map(ReviewMedia::getMediaUrl).toList() : List.of())
                .build();
    }
    public Map<String, Object> checkCanReview(Long userId, Long bookingId, Long roomId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return Map.of("canReview", false, "reason", "booking_not_found");
        if (booking.getUser() == null || !booking.getUser().getId().equals(userId))
            return Map.of("canReview", false, "reason", "not_authorized");
        if (booking.getStatus() != BookingStatus.CHECK_OUT)
            return Map.of("canReview", false, "reason", "not_checked_out");
        if (reviewRepository.existsByBookingIdAndRoomId(bookingId, roomId))
            return Map.of("canReview", false, "reason", "already_reviewed");
        return Map.of("canReview", true, "reason", "eligible");
    }

    @Transactional
    public ReviewResponseDTO toggleReviewVisibility(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá yêu cầu"));

        // Đảo ngược trạng thái: Nếu đang hiện (false) -> ẩn (true) và ngược lại
        review.setHidden(!review.isHidden());

        Review updated = reviewRepository.save(review);
        return mapToDTO(updated);
    }

}