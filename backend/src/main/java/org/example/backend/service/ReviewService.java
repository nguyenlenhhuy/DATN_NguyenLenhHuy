package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền đánh giá đơn hàng này");
        }

        if (reviewRepository.existsByBookingId(dto.getBookingId())) {
            throw new ReviewAlreadyExistsException("Đơn hàng này đã được đánh giá");
        }

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
        updateHotelStats(booking.getHotel().getId());
        return mapToDTO(saved);
    }

    // ================= DÀNH CHO ADMIN =================

    /**
     * Cập nhật phản hồi từ Admin/Staff cho đánh giá
     */
    @Transactional
    public ReviewResponseDTO replyReview(Long reviewId, String content) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá để phản hồi"));

        review.setReplyContent(content);
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
        // Thực hiện tính toán lại avg_rating và total_reviews trong bảng hotels
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