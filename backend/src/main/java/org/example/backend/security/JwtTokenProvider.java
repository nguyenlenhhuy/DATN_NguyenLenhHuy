package org.example.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.example.backend.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Spring tự động đọc chuỗi bí mật từ file application.properties
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Spring tự động đọc cấu hình thời gian sống (mili-giây)
    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    public String generateToken(User user) {
        // Đọc mảng byte từ chuỗi cấu hình động jwtSecret để tạo chữ ký bảo mật
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole().getRoleType().name())
                .setIssuedAt(new Date())
                // Sử dụng biến cấu hình động jwtExpirationInMs thay vì giá trị cố định cũ
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                // ĐÃ SỬA: Đổi từ HS2256 thành SignatureAlgorithm.HS256 chuẩn xác
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}