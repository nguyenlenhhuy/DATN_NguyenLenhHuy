package org.example.backend.repository;

import jakarta.persistence.LockModeType;
import org.example.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    // API Trang chủ: Lấy phòng ưu tiên (giả sử cột is_featured nằm ở Room hoặc RoomType)
    // Ở đây tôi viết query dựa trên field isFeatured bạn đã nêu
    @Query("SELECT r FROM Room r WHERE r.roomType.isFeatured = true")
    List<Room> findFeaturedRooms();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdWithLock(Long id);
}