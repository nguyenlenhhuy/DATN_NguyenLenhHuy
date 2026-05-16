package org.example.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RevenueTrendProjection {
    LocalDate getStatDate();
    BigDecimal getRevenue();
}