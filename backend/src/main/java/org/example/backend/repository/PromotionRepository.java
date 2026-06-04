package org.example.backend.repository;

import org.example.backend.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // 🔥 Sử dụng lại LocalDate chuẩn
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCodeIgnoreCaseAndIsActiveTrue(String code);

    // 🔥 ĐỔI TẠI ĐÂY: Chuyển LocalDateTime thành LocalDate để khớp 100% với Entity
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND :currentDate BETWEEN p.startDate AND p.endDate")
    List<Promotion> findAvailablePromotions(@Param("currentDate") LocalDate currentDate);
}