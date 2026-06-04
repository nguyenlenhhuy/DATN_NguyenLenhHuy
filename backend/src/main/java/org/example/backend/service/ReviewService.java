package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ReplyRequestDTO;
import org.example.backend.dto.request.ReviewRequestDTO;
import org.example.backend.dto.response.ReviewResponseDTO;
import org.example.backend.entity.*;
import org.example.backend.entity.enums.MediaType;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.exception.ReviewAlreadyExistsException;
import org.example.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final AuditLogRepository auditLogRepository;

    // ================= DÀNH CHO KHÁCH HÀNG =================

    @Transactional
    public ReviewResponseDTO submitReview(Long userId, ReviewRequestDTO dto) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        // 1. CHỐT CHẶN AN TOÀN: Đảm bảo Booking có chứa thông tin User trước khi xử lý tiếp
        if (booking.getUser() == null) {
            throw new IllegalStateException("Đơn đặt phòng này bị lỗi dữ liệu (không có thông tin khách hàng). Vui lòng kiểm tra lại Database.");
        }

        // 2. Kiểm tra quyền sở hữu đơn hàng (Lúc này gọi .getId() mới an toàn 100%)
        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền đánh giá đơn hàng này");
        }

        // 3. Kiểm tra nghiệp vụ khách đã check-out chưa
        if (booking.getStatus() == null || !booking.getStatus().name().equals("CHECKED_OUT")) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá sau khi đã hoàn tất thủ tục trả phòng (Check-out).");
        }

        // 4. Kiểm tra xem đã đánh giá chưa (Tránh spam)
        if (reviewRepository.existsByBookingId(dto.getBookingId())) {
            throw new ReviewAlreadyExistsException("Đơn hàng này đã được đánh giá");
        }

        // 5. Map dữ liệu và lưu
        Review review = new Review();
        review.setBooking(booking);
        review.setUser(booking.getUser());
        review.setHotel(booking.getHotel());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        if (dto.getMediaUrls() != null) {
            List<ReviewMedia> mediaList = dto.getMediaUrls().stream()
                    .map(url -> new ReviewMedia(null, review, url,
                            url.toLowerCase().endsWith(".mp4") ? MediaType.VIDEO : MediaType.IMAGE))
                    .toList();
            review.setMediaList(mediaList);
        }

        Review saved = reviewRepository.save(review);

        // 6. Cập nhật lại thống kê sao của khách sạn
        updateHotelStats(booking.getHotel().getId());
        return mapToDTO(saved);
    }

    // ================= DÀNH CHO ADMIN / STAFF =================

    /**
     * Cập nhật phản hồi từ Admin/Staff cho đánh giá của khách hàng.
     */
    @Transactional
    public ReviewResponseDTO replyReview(Long reviewId, ReplyRequestDTO replyDTO) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá để phản hồi"));

        // Chỉ cập nhật nội dung reply, giữ nguyên rating và comment cũ của khách
        review.setReplyContent(replyDTO.getReplyContent());

        Review saved = reviewRepository.save(review);
        return mapToDTO(saved);
    }

    /**
     * Admin xóa đánh giá vi phạm
     */
    @Transactional
    public void deleteReviewByAdmin(Long reviewId, Long adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại"));

        Long hotelId = review.getHotel().getId();
        String description = "Admin xóa đánh giá ID: " + reviewId + " của khách " + review.getUser().getFullName();

        // Lưu nhật ký hệ thống
        AuditLog log = new AuditLog(adminId, "DELETE_REVIEW", reviewId, description);
        auditLogRepository.save(log);

        reviewRepository.delete(review);
        updateHotelStats(hotelId);
    }

    /**
     * Lấy danh sách đánh giá cho Admin quản lý
     */
    public Page<ReviewResponseDTO> findAllForAdmin(int page, int size) {
        return reviewRepository.findAll(PageRequest.of(page, size))
                .map(this::mapToDTO);
    }

    // ================= HÀM HỖ TRỢ =================

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
                .userName(review.getUser().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .replyContent(review.getReplyContent())
                .createdAt(review.getCreatedAt())
                .mediaUrls(review.getMediaList() != null ?
                        review.getMediaList().stream().map(ReviewMedia::getMediaUrl).toList() : List.of())
                .build();
    }
}