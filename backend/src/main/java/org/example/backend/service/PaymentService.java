package org.example.backend.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.entity.Booking;
import org.example.backend.entity.DailyRevenueStat;
import org.example.backend.entity.Invoice;
import org.example.backend.entity.enums.BookingStatus;
import org.example.backend.entity.enums.PaymentStatus;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.BookingRepository;
import org.example.backend.repository.DailyRevenueStatRepository;
import org.example.backend.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PayOS payOS;
    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;
    private final DailyRevenueStatRepository revenueStatRepository;


    @Transactional
    public String createPaymentLink(Long invoiceId) throws Exception {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn không tồn tại: " + invoiceId));

        // PayOS yêu cầu danh sách món hàng
        ItemData item = ItemData.builder()
                .name("Thanh toán đặt phòng #" + invoice.getBooking().getId())
                .quantity(1)
                .price(invoice.getAmountPaid().intValue())
                .build();

        PaymentData paymentData = PaymentData.builder()
                .orderCode(invoice.getId()) // ID hóa đơn dùng làm mã đơn hàng
                .amount(invoice.getAmountPaid().intValue())
                .description("HMS PAY " + invoice.getId())
                .returnUrl("http://localhost:4200/payment/success") // URL Frontend của bạn
                .cancelUrl("http://localhost:4200/payment/cancel")
                .items(Collections.singletonList(item))
                .build();

        CheckoutResponseData data = payOS.createPaymentLink(paymentData);
        return data.getCheckoutUrl();
    }

    /**
     * Xử lý cập nhật DB khi thanh toán thành công (Gọi từ Webhook)
     */
    @Transactional
    public void processPaymentSuccess(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + invoiceId));

        // Tránh xử lý lặp lại nếu Webhook gọi nhiều lần
        if (PaymentStatus.PAID.equals(invoice.getPaymentStatus())) {
            return;
        }

        // 1. Cập nhật Invoice (Sử dụng LocalDateTime thay cho java.util.Date)
        invoice.setPaymentStatus(PaymentStatus.PAID);
        invoice.setPaymentDate(LocalDateTime.now());

        // 2. Cập nhật Booking
        Booking booking = invoice.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);

        // 3. Cập nhật thống kê doanh thu
        updateDailyRevenue(invoice.getAmountPaid());

        invoiceRepository.save(invoice);
        bookingRepository.save(booking);

        log.info("Xử lý thanh toán thành công cho hóa đơn số: {}", invoiceId);
    }

    /**
     * Cập nhật bảng thống kê doanh thu hàng ngày
     */
    private void updateDailyRevenue(BigDecimal amount) {
        LocalDate today = LocalDate.now();

        // Sử dụng findById với LocalDate và Builder của Lombok
        DailyRevenueStat stat = revenueStatRepository.findById(today)
                .orElseGet(() -> DailyRevenueStat.builder()
                        .statDate(today)
                        .revenue(BigDecimal.ZERO)
                        .totalBookings(0)
                        .build());

        stat.setRevenue(stat.getRevenue().add(amount));
        stat.setTotalBookings(stat.getTotalBookings() + 1);

        revenueStatRepository.save(stat);
    }
}