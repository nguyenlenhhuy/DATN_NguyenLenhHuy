package org.example.backend.repository;

import org.example.backend.entity.RoomTypeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, Long> {

    // Bạn có thể viết thêm hàm này để sau này Frontend cần lấy danh sách ảnh của 1 loại phòng
    List<RoomTypeImage> findByRoomTypeId(Long roomTypeId);
}