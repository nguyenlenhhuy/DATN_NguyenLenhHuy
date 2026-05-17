package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ChangePasswordRequest;
import org.example.backend.dto.response.UserProfileResponse;
import org.example.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Lấy thông tin cá nhân hiện tại
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(java.security.Principal principal) { // Dùng Principal
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Bạn chưa đăng nhập hoặc phiên làm việc hết hạn.");
        }
        String username = principal.getName(); // Lấy starkinggame138@gmail.com
        UserProfileResponse response = userService.getProfileByUsername(username);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateProfile(
            java.security.Principal principal, // Thay đổi ở đây
            @RequestBody UserProfileResponse updateRequest) {

        if (principal == null) return ResponseEntity.status(401).build();
        userService.updateProfile(principal.getName(), updateRequest);
        return ResponseEntity.ok("Thông tin cá nhân đã được cập nhật thành công.");
    }

    /**
     * Đổi mật khẩu
     */
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            java.security.Principal principal, // CHỈNH SỬA: Dùng Principal thay cho UserDetails
            @RequestBody ChangePasswordRequest request) {

        // Kiểm tra an toàn để tránh NullPointerException
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Phiên làm việc hết hạn, vui lòng đăng nhập lại.");
        }

        String username = principal.getName(); // Lấy username từ Principal đã xác thực

        try {
            userService.changePassword(username, request);
            return ResponseEntity.ok("Mật khẩu của bạn đã được cập nhật thành công.");
        } catch (RuntimeException e) {
            // Trả về thông báo lỗi cụ thể (ví dụ: mật khẩu cũ không khớp)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}