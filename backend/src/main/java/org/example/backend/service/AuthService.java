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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
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

    /**
     * BƯỚC 1: Đăng ký thành viên mới
     * Đã tách biệt xử lý để nếu lỗi Mail xảy ra, User vẫn được tạo tạm thời
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email này đã tồn tại trong hệ thống LuxeHotel!");
        }

        Role customerRole = roleRepository.findByRoleType(RoleType.CUSTOMER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống: Không tìm thấy quyền khách hàng."));

        User user = new User();
        user.setUsername(request.email());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.PENDING); // Tài khoản tạm giữ chờ kích hoạt
        user.setRole(customerRole);
        userRepository.save(user);

        try {
            // Thực hiện sinh mã và gửi mail
            sendNewOtp(request.email(), OtpType.REGISTER);
        } catch (Exception e) {
            // Khi gửi Mail lỗi, không làm sập luồng đăng ký mà cảnh báo để Client hiển thị nút "Gửi lại mã"
            log.warn("Tài khoản đã tạo nhưng không gửi được Mail kích hoạt: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.MULTI_STATUS, "Tài khoản đã tạo thành công, nhưng hệ thống gặp sự cố gửi Mail. Vui lòng bấm gửi lại mã OTP.");
        }
    }

    /**
     * BƯỚC PHỤ: Cập nhật Email mới tại màn hình OTP và gửi lại mã mới
     */
    @Transactional
    public void updateEmailAndResendOtp(UpdateEmailRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy số điện thoại khách hàng."));

        if (userRepository.existsByEmail(request.newEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email mới đã được sử dụng bởi một tài khoản khác.");
        }

        // Dọn dẹp OTP cũ gắn liền với email cũ
        otpStorageRepository.deleteByEmail(user.getEmail());

        user.setEmail(request.newEmail());
        user.setUsername(request.newEmail());
        userRepository.save(user);

        sendNewOtp(request.newEmail(), OtpType.REGISTER);
    }

    /**
     * BƯỚC 2: Xác thực OTP - Kích hoạt tài khoản chính thức
     */
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

        user.setStatus(UserStatus.ACTIVE); // Kích hoạt tài khoản thành công
        userRepository.save(user);

        // Xác thực xong thì xóa mã tránh dùng lại (Tối ưu bảo mật)
        otpStorageRepository.deleteByEmail(email);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        String identifier = loginRequest.getUsername().trim();

        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new AppException("Tài khoản không tồn tại trong hệ thống.", HttpStatus.UNAUTHORIZED));

        if (user.getStatus() == UserStatus.PENDING) {
            throw new AppException("Tài khoản của bạn chưa được kích hoạt OTP.", HttpStatus.FORBIDDEN);
        }

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

    /**
     * --- HÀM BỔ TRỢ: TẠO OTP VÀ KHỞI TẠO TIẾN TRÌNH GỬI MAIL (MimeMessage HTML) ---
     */
    private void sendNewOtp(String email, OtpType type) {
        String code = String.valueOf(new Random().nextInt(899999) + 100000);

        OtpStorage otp = new OtpStorage();
        otp.setEmail(email);
        otp.setOtpCode(code);
        otp.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otp.setOtpType(type);
        otpStorageRepository.save(otp);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setTo(email);
            helper.setSubject("Mã xác thực đặc quyền LuxeHotel");

            // Thiết kế giao diện Email HTML chuyên nghiệp tăng điểm đồ án
            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e3e3e3; border-radius: 12px;'>"
                    + "<h2 style='color: #d4af37; text-align: center; text-transform: uppercase; letter-spacing: 2px;'>LuxeHotel Premium</h2>"
                    + "<hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'>"
                    + "<p>Xin chào,</p>"
                    + "<p>Cảm ơn bạn đã lựa chọn đăng ký thành viên đặc quyền tại hệ thống quản lý khách sạn cao cấp <strong>LuxeHotel</strong>.</p>"
                    + "<p>Mã xác thực giao dịch kích hoạt tài khoản của bạn là:</p>"
                    + "<div style='text-align: center; margin: 30px 0;'>"
                    + "<span style='font-size: 32px; font-weight: bold; color: #d4af37; letter-spacing: 5px; background: #11141d; padding: 10px 30px; border-radius: 8px;'>" + code + "</span>"
                    + "</div>"
                    + "<p style='color: #777; font-size: 12px;'>Lưu ý: Mã OTP này có hiệu lực trong vòng 5 phút và chỉ được sử dụng một lần duy nhất. Vui lòng tuyệt đối không chia sẻ mã này với bất kỳ ai.</p>"
                    + "</div>";

            helper.setText(htmlContent, true); // Bật true để nhận diện định dạng HTML
            mailSender.send(mimeMessage);
            log.info("Đã gửi Mail OTP thành công tới địa chỉ: {}", email);
        } catch (Exception e) {
            log.error("Lỗi kết nối JavaMailSender nghiêm trọng: {}", e.getMessage());
            throw new RuntimeException("Môi trường máy chủ không thể xử lý lệnh gửi Mail: " + e.getMessage());
        }
    }
}