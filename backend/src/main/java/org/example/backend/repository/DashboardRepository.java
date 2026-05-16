package org.example.backend.repository;

import org.example.backend.dto.response.BookingStatusProjection;
import org.example.backend.dto.response.RevenueTrendProjection;
import org.example.backend.entity.DailyRevenueStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository
public interface DashboardRepository extends JpaRepository<DailyRevenueStat, LocalDate> {
    @Query(value = "SELECT status, COUNT(*) as count FROM bookings GROUP BY status", nativeQuery = true)
    List<BookingStatusProjection> getBookingStatusStatistics();

    @Query(value = "SELECT stat_date as statDate, revenue FROM daily_revenue_stats WHERE stat_date BETWEEN :start AND :end ORDER BY stat_date ASC", nativeQuery = true)
    List<RevenueTrendProjection> getRevenueTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = "SELECT (COUNT(CASE WHEN status = 'OCCUPIED' THEN 1 END) * 100.0 / COUNT(*)) FROM rooms", nativeQuery = true)
    Double getCurrentOccupancyRate();
}