package org.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.backend.entity.enums.UserStatus;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String roleName;
    private UserStatus status;
    private LocalDateTime createdAt;
}