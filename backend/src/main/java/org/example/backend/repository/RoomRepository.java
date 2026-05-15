package org.example.backend.repository;

import jakarta.persistence.LockModeType;
import org.example.backend.entity.Room;
import org.example.backend.entity.enums.RoomStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    @Query("SELECT r FROM Room r WHERE r.roomType.isFeatured = true")
    List<Room> findFeaturedRooms();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdWithLock(@Param("id") Long id);

    /**
     * Dùng @Query để tránh lỗi resolve attribute hotelId
     */
    @Query("SELECT COUNT(r) > 0 FROM Room r WHERE r.roomType.hotel.id = :hotelId AND r.roomNumber = :roomNumber")
    boolean existsByHotelIdAndRoomNumber(@Param("hotelId") Long hotelId, @Param("roomNumber") String roomNumber);

    /**
     * SỬA LỖI: Thêm dấu gạch dưới để JPA hiểu là roomType.id
     * Hoặc dùng @Query để tường minh hơn
     */
    @Query("SELECT COUNT(r) > 0 FROM Room r WHERE r.roomType.id = :roomTypeId")
    boolean existsByRoomTypeId(@Param("roomTypeId") Long roomTypeId);

    @Modifying
    @Query("UPDATE Room r SET r.status = :status WHERE r.id IN " +
            "(SELECT bd.room.id FROM BookingDetail bd WHERE bd.booking.id = :bookingId)")
    void updateStatusByBookingId(@Param("bookingId") Long bookingId, @Param("status") RoomStatus status);
}