package org.example.backend.dto.request;

public record VerifyOtpRequest(
        String email,
        String otpCode
) {}