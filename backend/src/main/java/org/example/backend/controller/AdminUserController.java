package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.AdminCreateUserRequest;
import org.example.backend.dto.request.RegisterRequest;
import org.example.backend.entity.User;
import org.example.backend.entity.enums.UserStatus;
import org.example.backend.repository.RoleRepository;
import org.example.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS})

public class AdminUserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    // 1. THÊM MỚI: API Lấy danh sách toàn bộ người dùng
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        // Giả sử trong UserService của bạn đã có hàm getAllUsers() gọi từ JpaRepository
        return ResponseEntity.ok(userService.getAllUsers());
    }
    // Thay đổi trạng thái (Active/Locked)
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> changeStatus(@PathVariable Long id, @RequestParam("status") String status) {
        // Nhận String thay vì Enum UserStatus trực tiếp để tránh lỗi convert
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
    // 1. API Thêm nhân viên mới (Staff)
    @PostMapping("/create-staff")
    public ResponseEntity<String> createStaff(@RequestBody AdminCreateUserRequest request) {
        // Logic: Kiểm tra email/username tồn tại -> Mã hóa pass -> Gán Role STAFF -> Save
        userService.createStaffAccount(request);
        return ResponseEntity.ok("Tạo tài khoản nhân viên thành công!");
    }

    // 2. API Cập nhật Role (Dùng để nâng cấp Customer lên Staff hoặc ngược lại)
    @PatchMapping("/{id}/role")
    public ResponseEntity<String> updateRole(@PathVariable Long id, @RequestParam("newRole") String newRole) {
        // Đảm bảo tên @RequestParam khớp với key trong HttpParams của Angular
        userService.updateUserRole(id, newRole);
        return ResponseEntity.ok("Cập nhật quyền hạn thành công");
    }
}