package org.example.backend.repository;

import jakarta.persistence.LockModeType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
            "JOIN b.bookingDetails bd " +
            "WHERE bd.room.id = :roomId " +
            "AND b.status IN (" +
            "org.example.backend.entity.enums.BookingStatus.PENDING, " +
            "org.example.backend.entity.enums.BookingStatus.CONFIRMED, " +
            "org.example.backend.entity.enums.BookingStatus.CHECK_IN) " +
            "AND (:checkIn < b.checkOutDate AND :checkOut > b.checkInDate)")
    boolean isRoomOccupied(@Param("roomId") Long roomId,
                           @Param("checkIn") LocalDate checkIn,
                           @Param("checkOut") LocalDate checkOut);
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}