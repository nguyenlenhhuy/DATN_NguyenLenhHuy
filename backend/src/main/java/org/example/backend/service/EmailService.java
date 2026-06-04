package org.example.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.example.backend.entity.Booking;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
}