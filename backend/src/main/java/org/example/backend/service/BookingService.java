package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.BookingRequest;
import org.example.backend.entity.*;
import org.example.backend.entity.enums.BookingStatus;
import org.example.backend.entity.enums.PaymentMethod;
import org.example.backend.entity.enums.PaymentStatus;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingDetailRepository bookingDetailRepository;

    /**
     * Chức năng Đặt phòng Online
     * Đảm bảo tính nguyên tử: Lưu Booking -> BookingDetail -> Invoice
     */
    @Transactional
    public Booking processBooking(BookingRequest request) {

        // 1. Khóa bản ghi Room để tránh tranh chấp (Concurrency)
        Room room = roomRepository.findByIdWithLock(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại!"));

        // 2. Kiểm tra ngày đặt có hợp lệ không
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new RuntimeException("Ngày đi phải sau ngày đến!");
        }

        // 3. Kiểm tra phòng trống (Double Check trong DB)
        boolean occupied = bookingRepository.isRoomOccupied(
                room.getId(), request.getCheckIn(), request.getCheckOut());

        if (occupied) {
            throw new RuntimeException("Phòng đã có người khác đặt trong khoảng thời gian này!");
        }

        // 4. Tính toán tổng chi phí dựa trên số đêm
        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal pricePerNight = room.getRoomType().getBasePrice();
        BigDecimal totalAmount = pricePerNight.multiply(new BigDecimal(nights));

        // 5. Khởi tạo Booking (Sử dụng đối tượng User proxy)
        Booking booking = new Booking();

        // Điều chỉnh quan trọng: Gán đối tượng User thay vì Long id
        User userProxy = new User();
        userProxy.setId(request.getUserId());
        booking.setUser(userProxy);

        booking.setCheckInDate(request.getCheckIn());
        booking.setCheckOutDate(request.getCheckOut());
        booking.setTotalPrice(totalAmount);
        booking.setStatus(BookingStatus.PENDING); // Trạng thái ban đầu

        // Lưu Booking để lấy ID
        Booking savedBooking = bookingRepository.save(booking);

        // 6. Lưu Chi tiết đặt phòng (Snapshot giá tại thời điểm đặt)
        BookingDetail detail = new BookingDetail();
        detail.setBooking(savedBooking);
        detail.setRoom(room);
        detail.setPriceAtBooking(pricePerNight);
        bookingDetailRepository.save(detail);

        // 7. Tạo hóa đơn (Invoice) chờ thanh toán
        Invoice invoice = new Invoice();
        invoice.setBooking(savedBooking);

        // Chuyển đổi String sang Enum an toàn
        try {
            invoice.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        } catch (IllegalArgumentException e) {
            invoice.setPaymentMethod(PaymentMethod.CASH); // Mặc định nếu truyền sai
        }

        invoice.setAmountPaid(BigDecimal.ZERO); // Chưa thanh toán
        invoice.setPaymentStatus(PaymentStatus.UNPAID);
        invoice.setCreatedAt(LocalDateTime.now());

        invoiceRepository.save(invoice);

        return savedBooking;
    }
}