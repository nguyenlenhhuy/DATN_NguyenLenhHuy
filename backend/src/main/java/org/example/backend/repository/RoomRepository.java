package org.example.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;
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

    // ĐÃ SỬA: Sửa đường dẫn thuộc tính Object r.hotel.id chuẩn JPQL
    @Query("SELECT COUNT(r) > 0 FROM Room r WHERE r.hotel.id = :hotelId AND r.roomNumber = :roomNumber")
    boolean existsByHotelIdAndRoomNumber(@Param("hotelId") Long hotelId, @Param("roomNumber") String roomNumber);

    @Query("SELECT COUNT(r) > 0 FROM Room r WHERE r.roomType.id = :roomTypeId")
    boolean existsByRoomTypeId(@Param("roomTypeId") Long roomTypeId);

    @Modifying
    @Transactional // Quan trọng cho các thao tác UPDATE
    @Query("UPDATE Room r SET r.status = :status WHERE r.id IN " +
            "(SELECT bd.room.id FROM BookingDetail bd WHERE bd.booking.id = :bookingId)")
    void updateRoomStatusByBookingId(@Param("bookingId") Long bookingId, @Param("status") RoomStatus status);

    // ĐÃ SỬA: Ép câu lệnh JPQL trỏ chuẩn vào r.hotel.id
    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId")
    List<Room> findByHotelId(@Param("hotelId") Long hotelId);

    // ĐÃ SỬA: Bọc @Query để dứt điểm lỗi khởi động ApplicationContext cho sơ đồ ma trận Angular
    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.isDeleted = false")
    List<Room> findByHotelIdAndIsDeletedFalse(@Param("hotelId") Long hotelId);

    // ĐÃ SỬA: Bọc @Query an toàn tuyệt đối cho luồng check phòng Tái sinh
    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.roomNumber = :roomNumber AND r.isDeleted = true")
    Optional<Room> findByHotelIdAndRoomNumberAndIsDeletedTrue(@Param("hotelId") Long hotelId, @Param("roomNumber") String roomNumber);

    Optional<Room> findByRoomNumber(String roomNumber);

    List<Room> findAllByStatus(RoomStatus status);
}