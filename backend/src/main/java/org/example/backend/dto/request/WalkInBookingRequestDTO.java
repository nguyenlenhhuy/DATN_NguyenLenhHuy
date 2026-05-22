package org.example.backend.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class WalkInBookingRequestDTO {
    private String roomNumber;
    private String customerName;
    private String customerPhone;
    private String customerCccd; // <-- ĐÃ BỔ SUNG: Tiếp nhận số định danh từ Angular gửi lên
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String appliedCode;
}