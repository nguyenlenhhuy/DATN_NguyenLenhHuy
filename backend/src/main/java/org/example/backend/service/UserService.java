package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ChangePasswordRequest;
import org.example.backend.dto.response.UserProfileResponse;
import org.example.backend.entity.User;
import org.example.backend.entity.enums.RoleType;
import org.example.backend.entity.enums.UserStatus;
import org.example.backend.exception.AppException;
import org.example.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // --- CÁC HÀM CÓ SẴN CỦA BẠN ---

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Không tìm thấy tài khoản người dùng!", HttpStatus.NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException("Tài khoản của bạn hiện đang bị khóa!", HttpStatus.FORBIDDEN);
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roleName(user.getRole().getRoleType().name())
                .status(user.getStatus().name())
                .build();
    }

    // Thêm vào UserService.java

    /**
     * Khách hàng tự cập nhật thông tin cá nhân (Họ tên, SĐT)
     */
    // Thêm vào UserService.java
    @Transactional
    public void updateProfile(String username, UserProfileResponse updateRequest) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new org.example.backend.exception.AppException(
                        "Không tìm thấy tài khoản để cập nhật!", org.springframework.http.HttpStatus.NOT_FOUND));

        // Cập nhật thông tin từ request gửi lên
        user.setFullName(updateRequest.getFullName());
        user.setPhone(updateRequest.getPhone());

        userRepository.save(user);
    }
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Không tìm thấy tài khoản người dùng!", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new AppException("Mật khẩu cũ không chính xác!", HttpStatus.BAD_REQUEST);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new AppException("Mật khẩu mới không được trùng với mật khẩu cũ hiện tại!", HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    // --- CÁC HÀM ADMIN QUẢN LÝ MỚI THÊM VÀO ---

    /**
     * Lấy danh sách tài khoản cho Admin (Phân trang + Lọc Role + Tìm kiếm)
     * Chỉ lấy các tài khoản chưa bị đánh dấu xóa (isDeleted = false)
     */
    @Transactional(readOnly = true)
    public Page<User> getUsersForAdmin(RoleType roleType, String keyword, Pageable pageable) {
        // Hàm searchUsers cần được thêm vào UserRepository như đã thảo luận
        return userRepository.searchUsersForAdmin(roleType, keyword, pageable);
    }

    /**
     * Thay đổi trạng thái hoạt động (Mở khóa hoặc Khóa tạm thời)
     */
    @Transactional
    public void updateAccountStatus(Long userId, UserStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Không tìm thấy tài khoản ID: " + userId, HttpStatus.NOT_FOUND));

        user.setStatus(newStatus);
        userRepository.save(user);
    }

    /**
     * Ngừng hoạt động tài khoản (Soft Delete)
     * Không xóa khỏi DB để giữ toàn vẹn dữ liệu lịch sử khách sạn.
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Không tìm thấy tài khoản cần ngừng hoạt động!", HttpStatus.NOT_FOUND));

        // Đánh dấu trạng thái ngừng hoạt động
        user.setIsDeleted(true);
        user.setStatus(UserStatus.LOCKED); // Khóa quyền truy cập ngay lập tức

        userRepository.save(user);
    }
}