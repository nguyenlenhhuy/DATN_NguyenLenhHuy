package org.example.backend.repository;


import org.example.backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    // Tìm hóa đơn theo ID đặt phòng (hữu ích khi xử lý thanh toán VNPAY trả về)
    Optional<Invoice> findByBookingId(Long bookingId);
    @Query("SELECT i FROM Invoice i JOIN FETCH i.booking WHERE i.id = :id")
    Optional<Invoice> findByIdWithBooking(@Param("id") Long id);
    @Query("SELECT SUM(i.amountPaid) FROM Invoice i WHERE i.paymentStatus = 'PAID' AND i.paymentDate BETWEEN :start AND :end")
    BigDecimal calculateTotalRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.paymentDate BETWEEN :start AND :end")
    Integer countTotalBookings(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

}