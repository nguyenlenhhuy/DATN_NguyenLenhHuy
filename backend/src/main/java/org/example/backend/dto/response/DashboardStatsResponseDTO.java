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
    private BigDecimal totalRevenueToday;    // Doanh thu hôm nay (luôn cố định)
    private BigDecimal totalRevenuePeriod;   // Doanh thu của kỳ đang xem (DATE/WEEK/MONTH/YEAR)
    private long totalBookingsToday;
    private long availableRooms;
    private long totalRooms;
    private Map<String, BigDecimal> last7DaysRevenue;
}