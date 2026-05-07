package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ForgotPasswordRequest;
import org.example.backend.dto.request.LoginRequest;
import org.example.backend.dto.request.RegisterRequest;
import org.example.backend.dto.request.ResetPasswordRequest;
import org.example.backend.dto.response.AuthResponse;
import org.example.backend.entity.OtpStorage;
import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.entity.enums.OtpType;
import org.example.backend.entity.enums.RoleType;
import org.example.backend.entity.enums.UserStatus;
import org.example.backend.repository.OtpStorageRepository;
import org.example.backend.repository.RoleRepository;
import org.example.backend.repository.UserRepository;
import org.example.backend.security.JwtUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpStorageRepository otpStorageRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final RoleRepository roleRepository;
    private final JwtUtils jwtUtils;
    @Transactional
    public void register(RegisterRequest request) {
        // 1. Kiểm tra trùng lặp
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại!");
        }
        Role customerRole = roleRepository.findByRoleType(RoleType.CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Lỗi: Quyền CUSTOMER chưa được khởi tạo trong DB!"));
        // 2. Lưu User với trạng thái PENDING
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.PENDING);
        user.setRole(customerRole);
        // Giả sử bạn đã tìm Role từ roleId và set vào đây
        userRepository.save(user);

        // 3. Tạo mã OTP 6 số
        String otpCode = String.valueOf(new Random().nextInt(899999) + 100000);

        // 4. Lưu OTP vào Storage (hết hạn sau 5 phút)
        OtpStorage otp = new OtpStorage();
        otp.setEmail(request.email());
        otp.setOtpCode(otpCode);
        otp.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otp.setOtpType(OtpType.REGISTER); // Giả sử enum có loại này
        otpStorageRepository.save(otp);

        // 5. Gửi Mail
        sendOtpEmail(request.email(), otpCode);
    }

    @Transactional
    public void verifyOtp(String email, String code) {
        OtpStorage otp = otpStorageRepository.findFirstByEmailAndOtpTypeOrderByExpiryTimeDesc(email, OtpType.REGISTER)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã OTP!"));

        // Kiểm tra hết hạn
        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn!");
        }

        // Kiểm tra khớp mã
        if (!otp.getOtpCode().equals(code)) {
            throw new RuntimeException("Mã OTP không chính xác!");
        }

        // Cập nhật User thành ACTIVE
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // Xóa OTP sau khi dùng xong
        otpStorageRepository.deleteByEmail(email);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        // Kiểm tra trạng thái: ACTIVE mới được vào
        if (Boolean.TRUE.equals(user.getIsDeleted()) || user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Tài khoản đã bị khóa hoặc chưa kích hoạt");
        }

        // Kiểm tra mật khẩu băm
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }

        // Tạo JWT Token
        String token = jwtUtils.generateToken(user);

        // Trả về DTO hoàn chỉnh cho Frontend
        return new AuthResponse(
                token,
                "Bearer",
                user.getUsername(),
                user.getRole().getRoleType().name()
        );
    }

    /**
     * Gửi OTP: Nhận vào ForgotPasswordRequest (chứa email)
     */
    @Transactional
    public void sendOtp(ForgotPasswordRequest request) {
        String email = request.getEmail();

        userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trên hệ thống"));

        String otp = String.format("%06d", new Random().nextInt(1000000));

        OtpStorage storage = new OtpStorage();
        storage.setEmail(email);
        storage.setOtpCode(otp);
        storage.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        storage.setOtpType(OtpType.FORGOT_PASSWORD);

        otpStorageRepository.save(storage);

        sendOtpEmail(email, otp);
    }

    /**
     * Đổi mật khẩu: Nhận vào ResetPasswordRequest (email, otp, newPassword)
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Tìm OTP mới nhất của email này
        OtpStorage storage = otpStorageRepository
                .findFirstByEmailAndOtpCodeAndOtpTypeOrderByExpiryTimeDesc(
                        request.getEmail(),
                        request.getOtp(),
                        OtpType.FORGOT_PASSWORD)
                .orElseThrow(() -> new RuntimeException("Mã OTP không hợp lệ hoặc đã sử dụng"));

        // Kiểm tra thời gian hết hạn
        if (storage.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn hiệu lực");
        }

        // Tìm User để cập nhật pass
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Người dùng không còn tồn tại"));

        // Mã hóa mật khẩu mới trước khi lưu
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xóa mã OTP để tránh dùng lại (Security Best Practice)
        otpStorageRepository.delete(storage);
    }

    private void sendOtpEmail(String email, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã xác thực khôi phục mật khẩu");
        message.setText("Mã OTP của bạn là: " + otpCode + ". Hiệu lực trong 5 phút.");
        mailSender.send(message);
    }
}