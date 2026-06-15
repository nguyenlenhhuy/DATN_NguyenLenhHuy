package org.example.backend.repository;

import org.example.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByHotelIdOrderByCreatedAtDesc(Long hotelId);

    boolean existsByBookingIdAndRoomId(Long bookingId, Long roomId);

    long countByBookingId(Long bookingId);

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.hotel.id = :hotelId ORDER BY r.createdAt DESC")
    Page<Review> findByHotelId(@Param("hotelId") Long hotelId, Pageable pageable);

    @Query("SELECT COUNT(r), AVG(r.rating) FROM Review r WHERE r.hotel.id = :hotelId")
    Object[] getRatingStats(@Param("hotelId") Long hotelId);

    // =========================================================================
    // 🔥 ĐÃ ĐIỀU CHỈNH: Thêm @Query định tuyến rõ ràng để chặt đứt lỗi sập PropertyReferenceException
    // =========================================================================
    @Query(value = "SELECT r.* FROM reviews r " +
            "INNER JOIN bookings b ON r.booking_id = b.id " +
            "INNER JOIN booking_details bd ON b.id = bd.booking_id " +
            "WHERE bd.room_id = :roomId " +
            "ORDER BY r.created_at DESC", nativeQuery = true)
    List<Review> findByBookingRoomIdOrderByCreatedAtDesc(@Param("roomId") Long roomId);
}