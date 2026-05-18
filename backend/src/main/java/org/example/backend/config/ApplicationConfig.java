package org.example.backend.config;

import lombok.RequiredArgsConstructor;
import org.example.backend.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {
    private final UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Phải là BCrypt
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return identifier -> userRepository.findByUsernameOrEmail(identifier, identifier)
                .map(user -> {
                    // Sử dụng getRoleType() theo đúng file Role.java bạn vừa gửi
                    // .name() sẽ trả về chuỗi "ADMIN", "STAFF" hoặc "CUSTOMER"
                    String roleName = user.getRole().getRoleType().name();
                    String authority = "ROLE_" + roleName;

                    return org.springframework.security.core.userdetails.User
                            .withUsername(user.getUsername())
                            .password(user.getPasswordHash()) // Cột password_hash trong DB
                            .authorities(authority) // Nạp đủ: ROLE_ADMIN, ROLE_STAFF, hoặc ROLE_CUSTOMER
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + identifier));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}