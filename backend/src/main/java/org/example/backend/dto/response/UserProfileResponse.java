package org.example.backend.dto.response;

import lombok.Builder;

@Builder
public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String roleName,
        String status
) {}