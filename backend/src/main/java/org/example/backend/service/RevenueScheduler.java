package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.entity.DailyRevenueStat;
import org.example.backend.repository.DashboardRepository;
import org.example.backend.repository.InvoiceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RevenueScheduler {
    private final InvoiceRepository invoiceRepository;
    private final DashboardRepository dashboardRepository;

    @Scheduled(cron = "0 55 23 * * ?")
    public void autoUpdateDailyStats() {
        LocalDate today = LocalDate.now();
        BigDecimal revenue = invoiceRepository.calculateTotalRevenue(today.atStartOfDay(), today.atTime(23,59,59));
        Integer bookings = invoiceRepository.countTotalBookings(today.atStartOfDay(), today.atTime(23,59,59));

        DailyRevenueStat stat = DailyRevenueStat.builder()
                .statDate(today)
                .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                .totalBookings(bookings != null ? bookings : 0)
                .updatedAt(LocalDateTime.now())
                .build();
        dashboardRepository.save(stat);
    }
}