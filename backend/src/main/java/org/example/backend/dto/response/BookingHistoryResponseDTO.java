package org.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.backend.entity.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class BookingHistoryResponseDTO {
    private Long bookingId;
    private String hotelName;
    private String hotelAddress;

    // 🔥 ĐÃ BỔ SUNG: Kích hoạt thuộc tính để Builder trong BookingService hết báo đỏ
    private String roomNumber;
    private String roomType;
    private Long roomId;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private boolean canReview; // Biến này để Frontend biết có nên hiện nút "Đánh giá" không
}