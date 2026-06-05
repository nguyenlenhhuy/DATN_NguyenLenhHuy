package org.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.entity.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor  // 🔥 ĐÃ THÊM: Tạo hàm dựng không tham số, bắt buộc phải có để Jackson mapping JSON không bị lỗi
@AllArgsConstructor // 🔥 ĐÃ THÊM: Đi kèm với @Builder và NoArgsConstructor để Lombok sinh mã chuẩn xác
public class BookingHistoryResponseDTO {
    private Long bookingId;
    private String hotelName;
    private String hotelAddress;

    // Các thuộc tính bóc tách dữ liệu phòng phẳng phục vụ Angular Timeline và Đặt lại
    private Long roomId;
    private String roomNumber;
    private String roomType;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private boolean canReview; // Biến này để Frontend biết có nên hiện nút "Đánh giá" không
}