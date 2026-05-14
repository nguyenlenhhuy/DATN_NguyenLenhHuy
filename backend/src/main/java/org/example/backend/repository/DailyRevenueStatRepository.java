package org.example.backend.repository;

import org.example.backend.entity.DailyRevenueStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DailyRevenueStatRepository extends JpaRepository<DailyRevenueStat, LocalDate> {
    // JpaRepository đã có sẵn findById và save, lỗi biến mất khi kiểu ID khớp
}