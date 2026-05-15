package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ChangePasswordRequest;
import org.example.backend.dto.response.UserProfileResponse;
import org.example.backend.entity.User;
import org.example.backend.entity.enums.UserStatus;
import org.example.backend.exception.CustomException;
import org.example.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // BCryptPasswordEncoder được inject từ SecurityConfig

    /**
     * Lấy thông tin Profile cá nhân dựa trên username trích xuất từ JWT
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUsername(String username) {
        // Tìm user trong DB bằng username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("Không tìm thấy tài khoản người dùng!", HttpStatus.NOT_FOUND));

        // Kiểm tra xem tài khoản có bị khóa không
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new CustomException("Tài khoản của bạn hiện đang bị khóa!", HttpStatus.FORBIDDEN);
        }

        // Map Entity sang DTO sạch để trả về (Bảo mật: tuyệt đối không trả về passwordHash)
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

    /**
     * Thay đổi mật khẩu chủ động (So khớp BCrypt mật khẩu cũ)
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        // Tìm user trong DB
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("Không tìm thấy tài khoản người dùng!", HttpStatus.NOT_FOUND));

        // 1. Dùng passwordEncoder.matches() để so khớp mật khẩu cũ thô với password_hash trong DB
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new CustomException("Mật khẩu cũ không chính xác!", HttpStatus.BAD_REQUEST);
        }

        // 2. Kiểm tra nghiệp vụ phụ: Tránh đổi mật khẩu mới trùng hệt mật khẩu cũ
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new CustomException("Mật khẩu mới không được trùng với mật khẩu cũ hiện tại!", HttpStatus.BAD_REQUEST);
        }

        // 3. Mã hóa mật khẩu mới bằng BCrypt và cập nhật lại vào Entity
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        // 4. Lưu lại vào DB (JPA tự động cập nhật trường updated_at nhờ hàm @PreUpdate ở Entity)
        userRepository.save(user);
    }
}