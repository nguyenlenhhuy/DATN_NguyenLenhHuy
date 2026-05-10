package org.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RoomTypeDetailResponse {
    private Long id;
    private String typeName;
    private BigDecimal basePrice;
    private Integer maxOccupancy;
    private String hotelName;
    private String address;
    private List<String> imageUrls;
}