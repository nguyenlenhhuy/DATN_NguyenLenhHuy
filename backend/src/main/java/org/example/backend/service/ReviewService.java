package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ReviewRequestDTO;
import org.example.backend.dto.response.ReviewResponseDTO;
import org.example.backend.entity.Booking;
import org.example.backend.entity.Review;
import org.example.backend.entity.ReviewMedia;
import org.example.backend.entity.enums.MediaType;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.exception.ReviewAlreadyExistsException;
import org.example.backend.repository.BookingRepository;
import org.example.backend.repository.HotelRepository;
import org.example.backend.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;

    @Transactional
    public ReviewResponseDTO submitReview(Long userId, ReviewRequestDTO dto) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền thực hiện thao tác này");
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
                            url.endsWith(".mp4") ? MediaType.VIDEO : MediaType.IMAGE))
                    .toList();
            review.setMediaList(mediaList);
        }

        Review saved = reviewRepository.save(review);
        hotelRepository.updateHotelRating(booking.getHotel().getId(), dto.getRating());

        return mapToDTO(saved);
    }

    private ReviewResponseDTO mapToDTO(Review review) {
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .userName(review.getUser().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .mediaUrls(review.getMediaList().stream().map(ReviewMedia::getMediaUrl).toList())
                .build();
    }
}