package org.example.backend.repository;

import org.example.backend.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCodeIgnoreCaseAndIsActiveTrue(String code);
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND :currentDate BETWEEN p.startDate AND p.endDate")
    List<Promotion> findAvailablePromotions(@Param("currentDate") LocalDate currentDate);
}