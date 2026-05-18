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
import org.example.backend.exception.AppException;
import org.example.backend.repository.OtpStorageRepository;
import org.example.backend.repository.RoleRepository;
import org.example.backend.repository.UserRepository;
import org.example.backend.security.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        // Kiểm tra Email tồn tại
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email này đã tồn tại trong hệ thống LuxeHotel!");
        }

        // Lấy quyền Customer
        Role customerRole = roleRepository.findByRoleType(RoleType.CUSTOMER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi cấu hình hệ thống: Không tìm thấy quyền khách hàng."));

        // Tạo User mới
        User user = new User();
        user.setUsername(request.email());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.PENDING);
        user.setRole(customerRole);
        userRepository.save(user);

        // Gửi OTP xác thực
        sendNewOtp(request.email(), OtpType.REGISTER);
    }

    @Transactional
    public void updateEmailAndResendOtp(UpdateEmailRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy số điện thoại khách hàng."));

        if (userRepository.existsByEmail(request.newEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email mới đã được sử dụng bởi một tài khoản khác.");
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
                .orElseThrow(() -> new AppException("Mã OTP không tồn tại hoặc đã bị xóa.", HttpStatus.BAD_REQUEST));

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new AppException("Mã OTP đã hết hạn, vui lòng yêu cầu mã mới.", HttpStatus.BAD_REQUEST);
        }

        if (!otp.getOtpCode().equals(code)) {
            throw new AppException("Mã xác thực không chính xác.", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Không tìm thấy thông tin người dùng.", HttpStatus.NOT_FOUND));

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        otpStorageRepository.deleteByEmail(email);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        String identifier = loginRequest.getUsername().trim();

        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new AppException("Tài khoản không tồn tại trong hệ thống.", HttpStatus.UNAUTHORIZED));

        if (user.getRole() == null) {
            throw new AppException("Tài khoản chưa được phân quyền.", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new AppException("Mật khẩu không chính xác.", HttpStatus.UNAUTHORIZED);
        }

        return new AuthResponse(jwtUtils.generateToken(user), "Bearer", user.getUsername(), user.getRole().getRoleType().name());
    }

    @Transactional
    public void sendOtp(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Địa chỉ Email chưa được đăng ký thành viên."));
        sendNewOtp(request.getEmail(), OtpType.FORGOT_PASSWORD);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        OtpStorage storage = otpStorageRepository.findFirstByEmailAndOtpCodeAndOtpTypeOrderByExpiryTimeDesc(
                        request.getEmail(), request.getOtp(), OtpType.FORGOT_PASSWORD)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã OTP khôi phục không hợp lệ."));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpStorageRepository.delete(storage);
    }

    // --- HÀM BỔ TRỢ GỬI MAIL ---
    private void sendNewOtp(String email, OtpType type) {
        try {
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
            message.setText("Mã xác nhận thành viên LuxeHotel của bạn là: " + code);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Lỗi gửi Mail: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể gửi mã xác thực tới Email này.");
        }
    }
}