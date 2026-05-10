package org.example.backend.dto.request;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RoomSearchRequest {
    private LocalDate checkIn;  // Bắt buộc
    private LocalDate checkOut; // Bắt buộc
    private Integer guestCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String typeName;
}