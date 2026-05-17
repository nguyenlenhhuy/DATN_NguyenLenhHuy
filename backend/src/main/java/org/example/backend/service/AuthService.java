package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.request.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OtpStorageRepository otpStorageRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final RoleRepository roleRepository;
    private final JwtUtils jwtUtils;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại!");
        }
        Role customerRole = roleRepository.findByRoleType(RoleType.CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Quyền CUSTOMER chưa có trong DB!"));

        User user = new User();
        user.setUsername(request.email());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.PENDING);
        user.setRole(customerRole);
        userRepository.save(user);

        sendNewOtp(request.email(), OtpType.REGISTER);
    }

    @Transactional
    public void updateEmailAndResendOtp(UpdateEmailRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SĐT này"));

        if (userRepository.existsByEmail(request.newEmail())) {
            throw new RuntimeException("Email mới đã được sử dụng");
        }

        otpStorageRepository.deleteByEmail(user.getEmail());
        user.setEmail(request.newEmail());
        user.setUsername(request.newEmail());
        userRepository.save(user);

        sendNewOtp(request.newEmail(), OtpType.REGISTER);
    }

    @Transactional
    public void verifyOtp(String email, String code) {
        OtpStorage otp = otpStorageRepository.findFirstByEmailAndOtpTypeOrderByExpiryTimeDesc(email, OtpType.REGISTER)
                .orElseThrow(() -> new RuntimeException("OTP không tồn tại"));

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) throw new RuntimeException("OTP hết hạn");
        if (!otp.getOtpCode().equals(code)) throw new RuntimeException("OTP sai");

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        otpStorageRepository.deleteByEmail(email);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        String identifier = loginRequest.getUsername().trim();
        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        if (user.getStatus() != UserStatus.ACTIVE) throw new RuntimeException("Tài khoản chưa kích hoạt");
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash()))
            throw new RuntimeException("Mật khẩu sai");

        return new AuthResponse(jwtUtils.generateToken(user), "Bearer", user.getUsername(), user.getRole().getRoleType().name());
    }

    // --- QUÊN MẬT KHẨU ---
    @Transactional
    public void sendOtp(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));
        sendNewOtp(request.getEmail(), OtpType.FORGOT_PASSWORD);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        OtpStorage storage = otpStorageRepository.findFirstByEmailAndOtpCodeAndOtpTypeOrderByExpiryTimeDesc(
                        request.getEmail(), request.getOtp(), OtpType.FORGOT_PASSWORD)
                .orElseThrow(() -> new RuntimeException("OTP không hợp lệ"));

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpStorageRepository.delete(storage);
    }

    // --- HÀM BỔ TRỢ ---
    private void sendNewOtp(String email, OtpType type) {
        String code = String.valueOf(new Random().nextInt(899999) + 100000);
        OtpStorage otp = new OtpStorage();
        otp.setEmail(email);
        otp.setOtpCode(code);
        otp.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otp.setOtpType(type);
        otpStorageRepository.save(otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã xác thực LuxeHotel");
        message.setText("Mã của bạn là: " + code);
        mailSender.send(message);
    }
}