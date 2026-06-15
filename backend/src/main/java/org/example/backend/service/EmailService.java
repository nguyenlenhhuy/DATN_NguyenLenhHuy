package org.example.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.example.backend.entity.Booking;
import org.example.backend.entity.Invoice;
import org.example.backend.entity.enums.PaymentMethod;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async // Chạy bất đồng bộ (ngầm) để khách quét QR xong không bị đứng màn hình chờ gửi mail
    public void sendBookingSuccessEmail(String toEmail, Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("🎉 LuxeHotel - Xác nhận đặt phòng thành công mã đơn #" + booking.getId());

            // Biên soạn nội dung thư bằng HTML nhìn cho chuyên nghiệp
            String htmlContent = "<div style='font-family: Arial, sans-serif; max-w: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 12px;'>"
                    + "<h2 style='color: #2563eb; text-align: center;'>CẢM ƠN BẠN ĐÃ ĐẶT PHÒNG!</h2>"
                    + "<p>Chào bạn, hệ thống LuxeHotel đã ghi nhận giao dịch thanh toán thành công của bạn.</p>"
                    + "<div style='background-color: #f8fafc; padding: 15px; border-radius: 8px; margin: 20px 0;'>"
                    + "<p><strong>Mã đơn hàng:</strong> #" + booking.getId() + "</p>"
                    + "<p><strong>Ngày Check-in:</strong> " + booking.getCheckInDate() + "</p>"
                    + "<p><strong>Ngày Check-out dự kiến:</strong> " + booking.getCheckOutDate() + "</p>"
                    + "<p style='color: #16a34a;'><strong>Trạng thái hóa đơn:</strong> ĐÃ THANH TOÁN (Qua PayOS)</p>"
                    + "<h3 style='color: #16a34a; margin-top: 10px;'>Tổng tiền: " + String.format("%,.0f", booking.getFinalAmount()) + " VND</h3>"
                    + "</div>"
                    + "<p style='font-size: 13px; color: #64748b;'>Vui lòng xuất trình email này hoặc mã đơn hàng tại quầy lễ tân khi đến nhận phòng để làm thủ tục nhanh nhất.</p>"
                    + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;'>"
                    + "<p style='text-align: center; font-size: 12px; color: #94a3b8;'>LuxeHotel - Hệ thống quản lý chuỗi khách sạn thông minh 2026</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("📩 [EMAIL] Đã gửi thư xác nhận thành công tới: " + toEmail);

        } catch (Exception e) {
            System.err.println("Lỗi gửi email xác nhận: " + e.getMessage());
        }
    }

    @Async
    public void sendInvoiceEmail(String toEmail, Booking booking, Invoice invoice,
                                 String roomNumber, String roomType, String hotelName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("🧾 LuxeHotel – Hóa đơn điện tử #LH-" + invoice.getId());

            long nights = Math.max(
                    java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate()), 1);

            boolean hasDiscount = booking.getDiscountAmount() != null
                    && booking.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0;

            String discountRow = hasDiscount
                    ? "<tr><td style='padding:10px 16px;color:#475569;'>Khuyến mãi / Voucher:</td>"
                      + "<td style='padding:10px 16px;text-align:right;color:#dc2626;font-weight:700;'>– "
                      + String.format("%,.0f", booking.getDiscountAmount()) + " ₫</td></tr>"
                    : "";

            String paymentDateStr = invoice.getPaymentDate() != null
                    ? invoice.getPaymentDate().toLocalDate().toString() : LocalDate.now().toString();

            String htmlContent =
                "<div style='font-family:Arial,Helvetica,sans-serif;max-width:640px;margin:0 auto;background:#f8fafc;border-radius:16px;overflow:hidden;border:1px solid #e2e8f0;'>"
                + "<div style='background:linear-gradient(135deg,#1e3a8a,#2563eb);padding:32px 36px;'>"
                + "<h1 style='margin:0;color:#fff;font-size:24px;letter-spacing:-0.5px;'>LuxeHotel</h1>"
                + "<p style='margin:4px 0 0;color:#bfdbfe;font-size:13px;'>Hệ thống quản lý khách sạn thông minh</p>"
                + "</div>"
                + "<div style='background:#fff;padding:24px 36px;border-bottom:1px solid #e2e8f0;'>"
                + "<div style='display:flex;justify-content:space-between;align-items:flex-start;'>"
                + "<div><p style='margin:0;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:1px;color:#64748b;'>Hóa đơn điện tử</p>"
                + "<h2 style='margin:4px 0 0;font-size:22px;font-weight:900;color:#1e293b;'>#LH-" + invoice.getId() + "</h2></div>"
                + "<div style='text-align:right;'><p style='margin:0;font-size:11px;color:#64748b;'>Ngày thanh toán</p>"
                + "<p style='margin:2px 0 0;font-size:14px;font-weight:700;color:#1e293b;'>" + paymentDateStr + "</p></div>"
                + "</div></div>"
                + "<div style='background:#fff;padding:24px 36px;'>"
                + "<h3 style='margin:0 0 16px;font-size:12px;font-weight:800;text-transform:uppercase;letter-spacing:0.5px;color:#64748b;'>Chi tiết lưu trú</h3>"
                + "<table style='width:100%;border-collapse:collapse;'>"
                + "<thead><tr style='background:#f1f5f9;'>"
                + "<th style='padding:10px 16px;text-align:left;font-size:11px;font-weight:700;text-transform:uppercase;color:#94a3b8;'>Dịch vụ</th>"
                + "<th style='padding:10px 16px;text-align:left;font-size:11px;font-weight:700;text-transform:uppercase;color:#94a3b8;'>Thời gian lưu trú</th>"
                + "<th style='padding:10px 16px;text-align:right;font-size:11px;font-weight:700;text-transform:uppercase;color:#94a3b8;'>Số đêm</th>"
                + "</tr></thead><tbody>"
                + "<tr>"
                + "<td style='padding:14px 16px;'>"
                + "<p style='margin:0;font-weight:700;color:#1e293b;'>Phòng " + roomNumber + " – " + roomType + "</p>"
                + "<p style='margin:2px 0 0;font-size:12px;color:#64748b;'>📍 " + hotelName + "</p>"
                + "</td>"
                + "<td style='padding:14px 16px;font-size:13px;color:#475569;'>"
                + booking.getCheckInDate() + " → " + booking.getCheckOutDate() + "</td>"
                + "<td style='padding:14px 16px;text-align:right;font-weight:700;color:#1e293b;'>" + nights + " đêm</td>"
                + "</tr></tbody></table>"
                + "<div style='margin-top:24px;border-top:2px solid #f1f5f9;'>"
                + "<table style='width:100%;border-collapse:collapse;margin-top:8px;'>"
                + "<tr><td style='padding:10px 16px;color:#475569;'>Giá phòng gốc:</td>"
                + "<td style='padding:10px 16px;text-align:right;color:#1e293b;font-weight:600;'>"
                + String.format("%,.0f", booking.getTotalPrice()) + " ₫</td></tr>"
                + discountRow
                + "<tr style='background:#f0fdf4;'>"
                + "<td style='padding:12px 16px;font-weight:800;font-size:15px;color:#15803d;'>Tổng thanh toán:</td>"
                + "<td style='padding:12px 16px;text-align:right;font-weight:900;font-size:18px;color:#15803d;'>"
                + String.format("%,.0f", booking.getFinalAmount()) + " ₫</td></tr>"
                + "</table></div>"
                + "<div style='margin-top:20px;background:#f8fafc;border-radius:10px;padding:14px 16px;border:1px solid #e2e8f0;'>"
                + "<p style='margin:0;font-size:12px;color:#64748b;'>Phương thức thanh toán: "
                + "<strong style='color:#1e293b;'>" + getPaymentMethodDisplay(invoice.getPaymentMethod()) + "</strong></p>"
                + "<p style='margin:6px 0 0;font-size:12px;color:#64748b;'>Trạng thái: "
                + "<span style='background:#dcfce7;color:#15803d;padding:2px 8px;border-radius:20px;font-size:11px;font-weight:700;'>✔ ĐÃ THANH TOÁN</span></p>"
                + "</div></div>"
                + "<div style='background:#f1f5f9;padding:20px 36px;text-align:center;'>"
                + "<p style='margin:0;font-size:12px;color:#64748b;font-weight:600;'>Vui lòng trình hóa đơn này hoặc mã đơn "
                + "<strong style='color:#1e3a8a;'>#LH-" + invoice.getId() + "</strong> tại quầy lễ tân khi đến nhận phòng.</p>"
                + "<p style='margin:8px 0 0;font-size:11px;color:#94a3b8;'>LuxeHotel — Hệ thống quản lý chuỗi khách sạn thông minh 2026</p>"
                + "</div></div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("📩 [EMAIL] Đã gửi hóa đơn tới: " + toEmail);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email hóa đơn: " + e.getMessage());
        }
    }

    private String getPaymentMethodDisplay(PaymentMethod method) {
        if (method == null) return "Không xác định";
        switch (method) {
            case PAYOS: return "PayOS (Thanh toán QR)";
            case VNPAY: return "VNPay";
            case CASH: return "Tiền mặt tại quầy";
            case BANK_TRANSFER: return "Chuyển khoản ngân hàng";
            default: return method.name();
        }
    }

    @Async
    public void sendPromotionNotificationEmail(
            String toEmail,
            String username,
            String roomNumber,
            String roomTypeName,
            String promoCode,
            Integer discountPercentage,
            LocalDate endDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("🔥 LuxeHotel - Phòng yêu thích của bạn vừa được giảm giá " + discountPercentage + "%!");

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 12px;'>"
                    + "<h2 style='color: #dc2626; text-align: center;'>🔥 TIN MỪNG! PHÒNG BẠN YÊU THÍCH VỪA ĐƯỢC GIẢM GIÁ!</h2>"
                    + "<p>Xin chào <strong>" + username + "</strong>,</p>"
                    + "<p>Phòng bạn đã lưu trong danh sách yêu thích vừa được áp mã ưu đãi mới!</p>"
                    + "<div style='background: linear-gradient(135deg, #fef2f2, #fee2e2); border: 2px solid #fca5a5; padding: 20px; border-radius: 12px; margin: 20px 0; text-align: center;'>"
                    + "<p style='font-size: 18px; font-weight: bold; color: #1e293b; margin: 0 0 8px 0;'>🛏️ Phòng " + roomNumber + " — " + roomTypeName + "</p>"
                    + "<p style='font-size: 36px; font-weight: 900; color: #dc2626; margin: 0;'>GIẢM " + discountPercentage + "%</p>"
                    + "<p style='font-size: 14px; color: #64748b; margin: 8px 0 0 0;'>Mã áp dụng: <strong style='color: #1d4ed8; background: #dbeafe; padding: 2px 8px; border-radius: 4px;'>" + promoCode + "</strong></p>"
                    + "<p style='font-size: 13px; color: #94a3b8; margin: 8px 0 0 0;'>Ưu đãi có hiệu lực đến: <strong>" + endDate + "</strong></p>"
                    + "</div>"
                    + "<div style='text-align: center; margin: 24px 0;'>"
                    + "<p style='font-size: 14px; color: #475569;'>Đừng để lỡ cơ hội này! Đặt ngay hôm nay để nhận được ưu đãi tốt nhất.</p>"
                    + "</div>"
                    + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;'>"
                    + "<p style='text-align: center; font-size: 12px; color: #94a3b8;'>LuxeHotel - Hệ thống quản lý chuỗi khách sạn thông minh 2026</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("📩 [EMAIL] Đã gửi email thông báo khuyến mãi tới: " + toEmail);

        } catch (Exception e) {
            System.err.println("Lỗi gửi email thông báo khuyến mãi: " + e.getMessage());
        }
    }
}