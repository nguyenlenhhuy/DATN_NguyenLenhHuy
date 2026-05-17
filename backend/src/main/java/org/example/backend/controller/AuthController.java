package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.*;
import org.example.backend.dto.response.AuthResponse;
import org.example.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Cho phép Angular truy cập
public class AuthController {

    private final AuthService authService;

    /**
     * Đăng ký tài khoản mới
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("Đăng ký thành công. Vui lòng kiểm tra email để lấy mã OTP.");
    }

    /**
     * Cập nhật Email mới và gửi lại mã OTP (Trường hợp nhập sai email lúc đăng ký)
     */
    @PutMapping("/update-email")
    public ResponseEntity<String> updateEmailAndResendOtp(@RequestBody UpdateEmailRequest request) {
        authService.updateEmailAndResendOtp(request);
        return ResponseEntity.ok("Đã cập nhật email và gửi lại mã OTP thành công!");
    }

    /**
     * Xác thực tài khoản bằng mã OTP
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody VerifyOtpRequest request) {
        // FIX: Đảm bảo VerifyOtpRequest có phương thức email() và otpCode() nếu là Record
        // Hoặc getEmail() và getOtpCode() nếu là Class
        authService.verifyOtp(request.email(), request.otpCode());
        return ResponseEntity.ok("Xác thực tài khoản thành công!");
    }

    /**
     * Đăng nhập hệ thống (Hỗ trợ Username/Email)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        // Xử lý logic tìm kiếm linh hoạt đã được cài đặt trong AuthService
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Gửi mã OTP khi quên mật khẩu
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        // ĐỔI TÊN: Sử dụng sendOtp thay vì sendOtpEmail để khớp với Service
        authService.sendOtp(request);
        return ResponseEntity.ok("Mã OTP đã được gửi thành công. Vui lòng kiểm tra email của bạn.");
    }

    /**
     * Đặt lại mật khẩu mới sau khi xác thực OTP
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Chúc mừng! Mật khẩu của bạn đã được thay đổi thành công.");
    }
}