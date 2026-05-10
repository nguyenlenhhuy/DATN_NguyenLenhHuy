package org.example.backend.repository;

import org.example.backend.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {

    // Tìm các chi tiết dựa trên ID của Booking (Hữu ích khi cần hiển thị hóa đơn chi tiết)
    List<BookingDetail> findByBookingId(Long bookingId);
}