package org.example.backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class WalkInBookingRequestDTO {
    private String roomNumber;      // backward compat (single room)
    private List<String> roomNumbers; // multi-room walk-in
    private String customerName;
    private String customerPhone;
    private String customerCccd;
    private String customerEmail; // tùy chọn — gửi hóa đơn qua email khi checkout
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String appliedCode;
}