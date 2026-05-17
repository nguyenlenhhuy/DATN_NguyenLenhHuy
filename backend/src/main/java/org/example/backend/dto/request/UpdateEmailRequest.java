package org.example.backend.dto.request;

public record UpdateEmailRequest(
        String phone,
        String oldEmail,
        String newEmail
) {}