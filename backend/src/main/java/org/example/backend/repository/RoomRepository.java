package org.example.backend.repository;

import jakarta.persistence.LockModeType;
import org.example.backend.entity.Room;
import org.example.backend.entity.enums.RoomStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    /**
     * Lấy danh sách phòng nổi bật dựa trên cấu hình loại phòng.
     * ĐÃ CẬP NHẬT: Thêm điều kiện r.isDeleted = false để tránh lấy ra các phòng đã xóa.
     */
    @Query("SELECT r FROM Room r WHERE r.roomType.isFeatured = true AND r.isDeleted = false")
    List<Room> findFeaturedRooms();

    /**
     * 🔥 HÀM DỰ PHÒNG (FALLBACK): Sử dụng khi chưa nhập dữ liệu 'isFeatured' trong DB.
     * Tự động lấy các phòng đang sẵn sàng phục vụ để lấp đầy giao diện Angular Home, tránh bị trống trang.
     */
    @Query("SELECT r FROM Room r WHERE r.isDeleted = false AND r.status = org.example.backend.entity.enums.RoomStatus.AVAILABLE")
    List<Room> findFeaturedRoomsFallback();

    /**
     * Khóa bi quan (Pessimistic Locking) - Ngăn chặn tình trạng tranh chấp phòng (Overbooking)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id AND r.isDeleted = false")
    Optional<Room> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT COUNT(r) > 0 FROM Room r WHERE r.hotel.id = :hotelId AND r.roomNumber = :roomNumber AND r.isDeleted = false")
    boolean existsByHotelIdAndRoomNumber(@Param("hotelId") Long hotelId, @Param("roomNumber") String roomNumber);

    @Query("SELECT COUNT(r) > 0 FROM Room r WHERE r.roomType.id = :roomTypeId AND r.isDeleted = false")
    boolean existsByRoomTypeId(@Param("roomTypeId") Long roomTypeId);

    /**
     * Cập nhật trạng thái phòng dựa trên ID của đơn đặt phòng (Booking)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Room r SET r.status = :status WHERE r.id IN " +
            "(SELECT bd.room.id FROM BookingDetail bd WHERE bd.booking.id = :bookingId) AND r.isDeleted = false")
    void updateRoomStatusByBookingId(@Param("bookingId") Long bookingId, @Param("status") RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.isDeleted = false")
    List<Room> findByHotelId(@Param("hotelId") Long hotelId);

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.isDeleted = false")
    List<Room> findByHotelIdAndIsDeletedFalse(@Param("hotelId") Long hotelId);

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.roomNumber = :roomNumber AND r.isDeleted = true")
    Optional<Room> findByHotelIdAndRoomNumberAndIsDeletedTrue(@Param("hotelId") Long hotelId, @Param("roomNumber") String roomNumber);

    @Query("SELECT r FROM Room r WHERE r.roomNumber = :roomNumber AND r.isDeleted = false")
    Optional<Room> findByRoomNumber(@Param("roomNumber") String roomNumber);

    @Query("SELECT r FROM Room r WHERE r.status = :status AND r.isDeleted = false")
    List<Room> findAllByStatus(@Param("status") RoomStatus status);

    /**
     * Giải phóng hàng loạt phòng vật lý thuộc các đơn đặt phòng bị hủy về trạng thái AVAILABLE.
     * Sử dụng câu lệnh sub-query lồng nhau để tối ưu hóa hiệu năng, giảm số lượng kết nối tới DB.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Room r SET r.status = :status WHERE r.id IN (" +
            "SELECT bd.room.id FROM BookingDetail bd WHERE bd.booking.id IN :bookingIds" +
            ") AND r.isDeleted = false")
    void releaseRoomsByBookingIds(@Param("status") RoomStatus status, @Param("bookingIds") List<Long> bookingIds);
}