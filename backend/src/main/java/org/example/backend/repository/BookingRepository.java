package org.example.backend.repository;

import jakarta.persistence.LockModeType;
import org.example.backend.entity.Booking;
import org.example.backend.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
            "JOIN b.bookingDetails bd " +
            "WHERE bd.room.id = :roomId " +
            "AND b.status IN (org.example.backend.entity.enums.BookingStatus.PENDING, " +
            "                 org.example.backend.entity.enums.BookingStatus.CONFIRMED, " +
            "                 org.example.backend.entity.enums.BookingStatus.CHECK_IN) " +
            "AND (:checkIn < b.checkOutDate AND :checkOut > b.checkInDate)")
    boolean isRoomOccupied(@Param("roomId") Long roomId,
                           @Param("checkIn") LocalDate checkIn,
                           @Param("checkOut") LocalDate checkOut);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.invoice LEFT JOIN FETCH b.user ORDER BY b.createdAt DESC")
    List<Booking> findAllBookingsWithInvoiceAndUser();

    @Query("SELECT b FROM Booking b " +
            "JOIN b.bookingDetails bd " +
            "WHERE bd.room.roomNumber = :roomNumber " +
            "AND b.status != org.example.backend.entity.enums.BookingStatus.CANCELLED")
    Optional<Booking> findByRoomNumber(@Param("roomNumber") String roomNumber);

    /**
     * 🔥 HÀM ĐƯỢC TÍCH HỢP ĐỒNG BỘ:
     * Tìm ra danh sách ID đơn hàng PENDING đã ngâm quá 5 phút để bàn giao cho Scheduler
     * thực thi lệnh giải phóng liên đới phòng vật lý và hủy hóa đơn.
     */
    @Query("SELECT b.id FROM Booking b WHERE b.status = :status AND b.createdAt < :threshold")
    List<Long> findExpiredBookingIds(@Param("status") BookingStatus status, @Param("threshold") LocalDateTime threshold);

    /**
     * 🔥 HÀM CỦA BẠN:
     * Tự động cập nhật tất cả các đơn hàng PENDING thành CANCELLED nếu thời gian tạo (createdAt)
     * vượt quá mốc thời gian threshold (Hiện tại trừ đi 5 phút).
     */
    @Modifying
    @Query("UPDATE Booking b SET b.status = org.example.backend.entity.enums.BookingStatus.CANCELLED " +
            "WHERE b.status = org.example.backend.entity.enums.BookingStatus.PENDING " +
            "AND b.createdAt < :threshold")
    int cancelExpiredBookings(@Param("threshold") LocalDateTime threshold);

}