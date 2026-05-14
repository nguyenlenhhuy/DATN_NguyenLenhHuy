package org.example.backend.repository;

import org.example.backend.entity.RoomPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface RoomPriceRepository extends JpaRepository<RoomPrice, Long> {
    List<RoomPrice> findByRoomTypeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long roomTypeId, LocalDate endDate, LocalDate startDate);
    @Query("SELECT rp FROM RoomPrice rp WHERE rp.roomType.id = :roomTypeId " +
            "AND rp.startDate <= :endDate AND rp.endDate >= :startDate")
    List<RoomPrice> findSpecialPrices(Long roomTypeId, LocalDate startDate, LocalDate endDate);
}