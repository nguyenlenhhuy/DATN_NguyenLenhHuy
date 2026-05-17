package org.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        // Log để Huy kiểm tra xem Token lấy ra có bị thừa dấu cách hay ngoặc kép không
        log.info("JWT nhận được: [{}]", jwt);

        try {
            if (jwt.split("\\.").length != 3) {
                log.warn("Token không đủ 3 phần từ IP: {}", request.getRemoteAddr());
                filterChain.doFilter(request, response);
                return;
            }

            username = jwtUtils.extractUsername(jwt);
            String role = jwtUtils.extractRole(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // ĐIỀU CHỈNH: Đảm bảo quyền có tiền tố ROLE_ để khớp với Spring Security
                String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(authority))
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("Xác thực thành công user: {} với quyền: {}", username, authority);
            }
        } catch (Exception e) {
            log.error("Lỗi xác thực JWT chi tiết: {}", e.getMessage());
            // Xóa Authentication nếu có lỗi để tránh dùng lại Context sai
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}