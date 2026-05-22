package org.example.backend.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

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

    // 🎯 BỔ SUNG CHÍNH XÁC 2 DÒNG NÀY VÀO FILE CỦA BẠN:
    private String appliedCode;
    private String paymentMethod;
}