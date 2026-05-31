package org.example.backend.repository;

import org.example.backend.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // Tìm kiếm bản ghi xem user đã thích phòng này chưa
    Optional<Favorite> findByUserIdAndRoomId(Long userId, Long roomId);

    // Lấy danh sách yêu thích của 1 user cụ thể
    List<Favorite> findByUserId(Long userId);

    // Kiểm tra nhanh tồn tại (Dùng để trả về trạng thái tim đỏ/tim trống ngoài danh sách)
    boolean existsByUserIdAndRoomId(Long userId, Long roomId);
}