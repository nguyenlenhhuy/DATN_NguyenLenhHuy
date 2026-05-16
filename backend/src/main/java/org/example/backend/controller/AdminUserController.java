package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.entity.User;
import org.example.backend.entity.enums.UserStatus;
import org.example.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    // Thay đổi trạng thái (Active/Locked)
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> changeStatus(@PathVariable Long id, @RequestParam UserStatus status) {
        // Sửa toggleAccountStatus thành updateAccountStatus
        userService.updateAccountStatus(id, status);
        return ResponseEntity.ok("Cập nhật trạng thái thành công");
    }

    // Ngừng hoạt động (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> stopActivity(@PathVariable Long id) {
        // Sửa softDeleteUser thành deactivateUser
        userService.deactivateUser(id);
        return ResponseEntity.ok("Tài khoản đã ngừng hoạt động");
    }
}