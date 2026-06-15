package org.example.backend.config;

import lombok.RequiredArgsConstructor;
import org.example.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // QUAN TRỌNG: Thêm import này
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 1. CHO PHÉP PREFLIGHT: Cho phép mọi yêu cầu OPTIONS đi qua không cần Token
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ====================================================================
                        // 2. NHÓM PUBLIC (Khách vãng lai xem thoải mái - Không cần Token)
                        // ====================================================================
                        .requestMatchers("/api/auth/**").permitAll()

                        // Khách được xem danh sách và chi tiết phòng, khách sạn
                        .requestMatchers(HttpMethod.GET, "/api/rooms/**", "/api/hotels/**").permitAll()

                        // FIX: Mở khóa cho phép khách vãng lai tải danh sách loại phòng ở trang chủ
                        .requestMatchers(HttpMethod.GET, "/api/v1/management/room-types").permitAll()

                        // FIX: Mở khóa cho phép khách tải danh sách bình luận của một phòng
                        .requestMatchers(HttpMethod.GET, "/api/reviews/room/**").permitAll()

                        // FIX: Mở khóa tính năng Chat AI cho mọi người trải nghiệm
                        .requestMatchers("/api/ai-chat/**", "/api/chat/**").permitAll()


                        // ====================================================================
                        // 3. NHÓM GIAO DỊCH & CÁ NHÂN (Yêu cầu đăng nhập, bất kể là Role gì)
                        // ====================================================================
                        // Chặn STAFF truy cập các endpoint quản trị đánh giá (chỉ ADMIN)
                        .requestMatchers("/api/reviews/admin/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                        // Nhóm này bắt buộc phải có tài khoản (Customer, Staff hoặc Admin đều được)
                        // Lưu ý: Liệt kê cả có tiền tố ROLE_ và không có tiền tố để tránh lỗi map dữ liệu
                        .requestMatchers("/api/users/**", "/api/bookings/**", "/api/favorites/**", "/api/reviews/**")
                        .hasAnyAuthority("CUSTOMER", "ROLE_CUSTOMER", "STAFF", "ROLE_STAFF", "ADMIN", "ROLE_ADMIN")


                        // ====================================================================
                        // 4. NHÓM QUẢN TRỊ NỘI BỘ (Chỉ dành cho Nhân viên và Quản lý)
                        // ====================================================================
                        // Bao gồm các endpoint quản lý đặt phòng, duyệt phòng, thêm sửa xóa phòng...
                        .requestMatchers("/api/v1/management/**")
                        .hasAnyAuthority("STAFF", "ROLE_STAFF", "ADMIN", "ROLE_ADMIN")


                        // ====================================================================
                        // 5. NHÓM ADMIN (Quyền lực cao nhất hệ thống)
                        // ====================================================================
                        .requestMatchers("/api/admin/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                        // Mọi yêu cầu khác không nằm trong danh sách trên đều phải có Token
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));

        // 2. KHAI BÁO PATCH: Bổ sung "PATCH" vào danh sách các phương thức được phép
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 3. MỞ RỘNG HEADERS: Thêm các header cần thiết cho việc xác thực và gửi dữ liệu
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Cache-Control",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Tối ưu: Lưu cấu hình CORS trong 1 giờ để giảm request OPTIONS dư thừa

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}