package org.example.backend.repository;


import org.example.backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    // Tìm hóa đơn theo ID đặt phòng (hữu ích khi xử lý thanh toán VNPAY trả về)
    Optional<Invoice> findByBookingId(Long bookingId);
    @Query("SELECT i FROM Invoice i JOIN FETCH i.booking WHERE i.id = :id")
    Optional<Invoice> findByIdWithBooking(@Param("id") Long id);
}