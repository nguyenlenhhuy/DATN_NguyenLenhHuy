package entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_revenue_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenueStat {
    @Id
    private LocalDate statDate;
    private BigDecimal revenue;
    private Integer totalBookings;
}