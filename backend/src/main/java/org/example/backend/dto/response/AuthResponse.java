package org.example.backend.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;

// 4. DTO trả về sau khi đăng nhập thành công
@AllArgsConstructor
@Data
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private String username;
    private String role;
}