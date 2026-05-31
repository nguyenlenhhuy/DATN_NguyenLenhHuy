package org.example.backend.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponseDTO {
    private BigDecimal totalRevenueToday;
    private long totalBookingsToday;
    private long availableRooms;
    private long totalRooms;
    private Map<String, BigDecimal> last7DaysRevenue; // Key: "yyyy-MM-dd", Value: Doanh thu
}