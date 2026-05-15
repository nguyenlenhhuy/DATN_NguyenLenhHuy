package org.example.backend.dto.request;

public record ChangePasswordRequest(
        String oldPassword,
        String newPassword
) {}