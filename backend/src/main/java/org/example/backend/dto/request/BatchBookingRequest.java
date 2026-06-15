package org.example.backend.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BatchBookingRequest {

    private Long userId;

    @NotEmpty(message = "Phải chọn ít nhất 1 phòng")
    private List<Long> roomIds;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @FutureOrPresent(message = "Ngày nhận phòng phải từ hôm nay trở đi")
    private LocalDate checkIn;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Future(message = "Ngày trả phòng phải ở tương lai")
    private LocalDate checkOut;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;

    private String couponCode;
}
