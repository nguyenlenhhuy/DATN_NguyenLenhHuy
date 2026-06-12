package org.example.backend.controller;

import org.example.backend.service.FavoriteService;
import org.example.backend.repository.UserRepository;
import org.example.backend.dto.response.FavoriteResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    public FavoriteController(FavoriteService favoriteService, UserRepository userRepository) {
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getFavoriteRooms(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Bạn cần đăng nhập để thực hiện chức năng này!");
        }

        String username = authentication.getName();
        org.example.backend.entity.User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại trong hệ thống!"));

        // 👉 CHỈ CẦN GỌI DÒNG NÀY LÀ XONG!
        // Mọi logic fix giá, số khách, khuyến mãi đã được chạy ngầm trong file FavoriteService
        List<FavoriteResponseDTO> dtoList = favoriteService.getFavoritesByUserId(currentUser.getId());

        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/toggle/{roomId}")
    public ResponseEntity<?> toggleFavorite(@PathVariable Long roomId, Authentication authentication) {
        // ... (Giữ nguyên code hàm toggleFavorite cũ của bạn) ...
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Bạn cần đăng nhập để thực hiện chức năng này!");
        }

        String username = authentication.getName();
        org.example.backend.entity.User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại trong hệ thống!"));

        boolean isFavorite = favoriteService.toggleFavorite(currentUser.getId(), roomId);

        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "isFavorite", isFavorite,
                "message", isFavorite ? "Đã thêm vào danh sách yêu thích" : "Đã xóa khỏi danh sách yêu thích"
        ));
    }
}