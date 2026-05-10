package org.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class RoomResponseDTO {
    private Long roomId;
    private String roomNumber;
    private String typeName;
    private BigDecimal price;
    private String hotelName;
}