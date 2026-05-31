package org.example.backend.controller;

import org.example.backend.service.FavoriteService;
import org.example.backend.repository.UserRepository;
import org.example.backend.repository.FavoriteRepository; // THÊM: Import Repository để bốc dữ liệu danh sách
import org.example.backend.entity.Favorite; // THÊM: Import Entity Favorite
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List; // THÊM: Import List

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository; // THÊM: Khai báo FavoriteRepository

    // Cập nhật Constructor để Spring tự động Inject cả 3 thành phần cần thiết vào
    public FavoriteController(FavoriteService favoriteService, UserRepository userRepository, FavoriteRepository favoriteRepository) {
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository; // Khởi tạo mapping repo
    }

    // =========================================================================
    // FIX DỨT ĐIỂM F5 MẤT TIM: Endpoint GET lấy toàn bộ danh sách phòng đã thích
    // =========================================================================
    @GetMapping
    public ResponseEntity<?> getFavoriteRooms(Authentication authentication) {
        // 1. Kiểm tra trạng thái đăng nhập hệ thống
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Bạn cần đăng nhập để thực hiện chức năng này!");
        }

        // 2. Bốc chuỗi username định danh từ token ra
        String username = authentication.getName();

        // 3. Tìm User trong DB để bốc ra UserId thật
        org.example.backend.entity.User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại trong hệ thống!"));

        // 4. Gọi hàm findByUserId có sẵn trong Repository của Huy để bốc dữ liệu lên
        List<Favorite> favorites = favoriteRepository.findByUserId(currentUser.getId());

        // 5. Trả về mảng danh sách cho Angular đối chiếu chéo thuật toán .includes()
        return ResponseEntity.ok(favorites);
    }

    // =========================================================================
    // ENDPOINT POST: Xử lý sự kiện click click bật/tắt tim
    // =========================================================================
    @PostMapping("/toggle/{roomId}")
    public ResponseEntity<?> toggleFavorite(@PathVariable Long roomId, Authentication authentication) {
        // Kiểm tra trạng thái đăng nhập
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Bạn cần đăng nhập để thực hiện chức năng này!");
        }

        // Bốc chuỗi username từ Principal ra
        String username = authentication.getName();

        // Tìm kiếm User thực tế bằng username để lấy ID
        org.example.backend.entity.User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại trong hệ thống!"));

        Long userId = currentUser.getId();

        // Gọi Service thực hiện toggle thêm/bớt yêu thích
        boolean isFavorite = favoriteService.toggleFavorite(userId, roomId);

        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "isFavorite", isFavorite,
                "message", isFavorite ? "Đã thêm vào danh sách yêu thích" : "Đã xóa khỏi danh sách yêu thích"
        ));
    }
}