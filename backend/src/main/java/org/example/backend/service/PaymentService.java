package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.entity.Booking;
import org.example.backend.entity.BookingDetail;
import org.example.backend.entity.DailyRevenueStat;
import org.example.backend.entity.Invoice;
import org.example.backend.entity.Room;
import org.example.backend.entity.enums.BookingStatus;
import org.example.backend.entity.enums.PaymentStatus;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.BookingDetailRepository;
import org.example.backend.repository.BookingRepository;
import org.example.backend.repository.DailyRevenueStatRepository;
import org.example.backend.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;

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
    private final BookingDetailRepository bookingDetailRepository;
    private final EmailService emailService;

    @Transactional
    public String createPaymentLink(Long invoiceId) throws Exception {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn không tồn tại: " + invoiceId));

        int totalAmount = invoice.getBooking().getFinalAmount().intValue();

        // PayOS yêu cầu danh sách món hàng
        ItemData item = ItemData.builder()
                .name("Thanh toán đặt phòng #" + invoice.getBooking().getId())
                .quantity(1)
                .price(totalAmount)
                .build();

        // 🔥 TÍNH TOÁN: Thời gian hiện tại + 5 phút (5 * 60 giây)
        long expiredAtInSeconds = (System.currentTimeMillis() / 1000) + 300;

        PaymentData paymentData = PaymentData.builder()
                .orderCode(invoice.getId())
                .amount(totalAmount)
                .description("HMS PAY " + invoice.getId())
                .returnUrl("http://localhost:4200/payment/success")
                .cancelUrl("http://localhost:4200/payment/cancel")
                .items(Collections.singletonList(item))
                .expiredAt(expiredAtInSeconds) // 🔥 ĐÂY RỒI: Truyền thời gian hết hạn sang PayOS
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

        // Lấy số tiền thực tế khách vừa thanh toán thành công qua PayOS
        BigDecimal actualAmount = invoice.getBooking().getFinalAmount();

        // ✅ SỬA BUG 2: Cập nhật trạng thái kèm theo điền số tiền thực tế vào hóa đơn
        invoice.setPaymentStatus(PaymentStatus.PAID);
        invoice.setAmountPaid(actualAmount); // Lưu lại số tiền để database ghi nhận chính xác
        invoice.setPaymentDate(LocalDateTime.now());

        // 2. Cập nhật Booking sang trạng thái đã xác nhận phòng
        Booking booking = invoice.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);

        // ✅ SỬA BUG 3: Truyền số tiền thực tế (actualAmount) vào thống kê doanh thu thay vì số 0 ban đầu
        updateDailyRevenue(actualAmount);

        invoiceRepository.save(invoice);
        bookingRepository.save(booking);

        // Trích xuất dữ liệu phòng trong transaction (trước khi session đóng)
        String toEmail = (booking.getUser() != null) ? booking.getUser().getEmail() : null;
        String roomNumber = "N/A", roomType = "N/A", hotelName = "LuxeHotel";
        try {
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(booking.getId());
            if (!details.isEmpty()) {
                Room room = details.get(0).getRoom();
                if (room != null) {
                    roomNumber = room.getRoomNumber();
                    if (room.getRoomType() != null) {
                        roomType = room.getRoomType().getTypeName();
                        if (room.getRoomType().getHotel() != null) {
                            hotelName = room.getRoomType().getHotel().getName();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Không thể lấy thông tin phòng cho email hóa đơn: {}", ex.getMessage());
        }

        // Gửi email hóa đơn sau khi transaction commit xong
        if (toEmail != null && !toEmail.isBlank()) {
            final String finalEmail = toEmail;
            final String finalRoom = roomNumber, finalType = roomType, finalHotel = hotelName;
            final Invoice finalInvoice = invoice;
            final Booking finalBooking = booking;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.sendInvoiceEmail(finalEmail, finalBooking, finalInvoice, finalRoom, finalType, finalHotel);
                }
            });
        }

        log.info("Xử lý thanh toán thành công cho hóa đơn số: {}. Số tiền: {}", invoiceId, actualAmount);
    }

    /**
     * Xác minh thanh toán PayOS qua API rồi cập nhật DB.
     * Gọi từ Angular sau khi PayOS redirect về returnUrl.
     */
    @Transactional
    public Map<String, Object> verifyAndProcessPayment(Long orderCode) {
        Invoice invoice = invoiceRepository.findById(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + orderCode));

        // Idempotent: đã PAID → trả về success ngay
        if (PaymentStatus.PAID.equals(invoice.getPaymentStatus())) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("bookingId", invoice.getBooking().getId());
            result.put("message", "Thanh toán đã được xác nhận trước đó.");
            return result;
        }

        try {
            PaymentLinkData paymentInfo = payOS.getPaymentLinkInformation(orderCode);
            if ("PAID".equalsIgnoreCase(paymentInfo.getStatus())) {
                processPaymentSuccess(orderCode);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("bookingId", invoice.getBooking().getId());
                result.put("message", "Thanh toán xác nhận thành công!");
                return result;
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("status", paymentInfo.getStatus());
                result.put("message", "PayOS báo trạng thái: " + paymentInfo.getStatus());
                return result;
            }
        } catch (Exception e) {
            log.warn("Không thể xác minh PayOS cho orderCode {}: {}", orderCode, e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Không thể xác minh với PayOS: " + e.getMessage());
            return result;
        }
    }

    /**
     * Cập nhật bảng thống kê doanh thu hàng ngày
     */
    private void updateDailyRevenue(BigDecimal amount) {
        LocalDate today = LocalDate.now();

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