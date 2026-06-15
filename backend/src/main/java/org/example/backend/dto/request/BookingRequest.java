package org.example.backend.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequest {

    // Sẽ lấy từ Token trong Controller, có thể không cần bắt buộc ở DTO
    private Long userId;

    @NotNull(message = "ID Phòng không được để trống")
    private Long roomId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @FutureOrPresent(message = "Ngày nhận phòng phải từ hôm nay trở đi")
    private LocalDate checkIn;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Future(message = "Ngày trả phòng phải ở tương lai")
    private LocalDate checkOut;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod; // PAYOS, CASH...
    private String couponCode;

    /** Token xác thực hold (lấy từ POST /api/rooms/{id}/hold). Tùy chọn — nếu không có hold, skip kiểm tra. */
    private String holdToken;
}