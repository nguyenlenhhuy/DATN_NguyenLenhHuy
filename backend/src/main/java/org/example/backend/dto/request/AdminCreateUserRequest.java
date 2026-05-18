package org.example.backend.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String phone;
}