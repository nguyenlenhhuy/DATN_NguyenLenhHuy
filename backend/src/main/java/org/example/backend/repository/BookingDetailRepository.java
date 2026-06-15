package org.example.backend.repository;

import org.example.backend.entity.BookingDetail;
import org.example.backend.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {

    List<BookingDetail> findByBookingId(Long bookingId);

    // Native SQL — bao gồm CHECK_OUT để hiển thị đúng công suất cho ngày quá khứ
    @Query(value = "SELECT COUNT(DISTINCT bd.room_id) FROM booking_details bd " +
                   "INNER JOIN bookings b ON bd.booking_id = b.id " +
                   "WHERE b.status IN ('CONFIRMED', 'CHECK_IN', 'CHECK_OUT') " +
                   "AND b.check_in_date <= :day AND b.check_out_date > :day",
           nativeQuery = true)
    long countOccupiedRoomsOnDay(@Param("day") LocalDate day);

    // JPQL với :statuses parameter — tránh enum literal hard-code trong IN clause
    @Query("SELECT bd FROM BookingDetail bd " +
           "JOIN FETCH bd.room r " +
           "JOIN FETCH r.roomType " +
           "JOIN FETCH bd.booking b " +
           "LEFT JOIN FETCH b.user " +
           "WHERE b.status IN :statuses " +
           "AND b.checkInDate <= :day AND b.checkOutDate > :day")
    List<BookingDetail> findOccupiedRoomsDetailOnDay(
            @Param("day") LocalDate day,
            @Param("statuses") List<BookingStatus> statuses);
}