package org.example.backend.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {
    private Long bookingId;
    private String roomNumber;
    private String customerName;
    private String customerPhone;
    private String customerCccd;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String bookingStatus;
    private String paymentStatus;

    private String appliedCode;
    private String paymentMethod;

    // Hỗ trợ multi-room booking: danh sách số phòng và số lượng phòng
    private List<String> roomNumbers;
    private int roomCount;
}