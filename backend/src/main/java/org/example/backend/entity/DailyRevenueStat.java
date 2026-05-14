package org.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_revenue_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRevenueStat {
    @Id
    @Column(name = "stat_date")
    private LocalDate statDate; // Ngày thống kê (Primary Key)

    @Column(name = "revenue")
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "total_bookings")
    private Integer totalBookings = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}