package org.example.backend.dto.response;

import lombok.*;

@Data // Annotation này sẽ tự tạo getPhone(), getFullName(),...
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String roleName;
    private String status;
}