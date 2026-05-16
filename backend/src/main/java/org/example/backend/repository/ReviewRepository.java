package org.example.backend.repository;

import org.example.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable; // ĐÚNG
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Tìm các đánh giá theo Hotel ID để hiển thị ở trang chi tiết khách sạn
    List<Review> findByHotelIdOrderByCreatedAtDesc(Long hotelId);

    boolean existsByBookingId(Long bookingId);
    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.hotel.id = :hotelId ORDER BY r.createdAt DESC")
    Page<Review> findByHotelId(@Param("hotelId") Long hotelId, Pageable pageable);
    @Query("SELECT COUNT(r), AVG(r.rating) FROM Review r WHERE r.hotel.id = :hotelId")
    Object[] getRatingStats(@Param("hotelId") Long hotelId);
}
