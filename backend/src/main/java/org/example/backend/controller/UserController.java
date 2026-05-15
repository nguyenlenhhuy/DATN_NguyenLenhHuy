package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ChangePasswordRequest;
import org.example.backend.dto.response.UserProfileResponse;
import org.example.backend.service.UserService; // Gọi thẳng đến lớp Service duy nhất
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService; // Khai báo trực tiếp lớp Service

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        UserProfileResponse response = userService.getProfileByUsername(username);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {

        String username = userDetails.getUsername();
        userService.changePassword(username, request);
        return ResponseEntity.ok("Mật khẩu của bạn đã được cập nhật thành công.");
    }
}