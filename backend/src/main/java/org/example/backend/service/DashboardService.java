package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.repository.DashboardRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class DashboardService {
    private final DashboardRepository dashboardRepository;

    public Map<String, Object> getAdminStats(LocalDate start, LocalDate end) {
        Map<String, Object> data = new HashMap<>();
        data.put("revenueTrend", dashboardRepository.getRevenueTrend(start, end));
        data.put("bookingStatus", dashboardRepository.getBookingStatusStatistics());
        data.put("occupancyRate", dashboardRepository.getCurrentOccupancyRate());
        return data;
    }
}
