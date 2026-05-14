package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ReviewRequestDTO;
import org.example.backend.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/submit")
    public ResponseEntity<?> createReview(@RequestBody ReviewRequestDTO reviewDTO, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        reviewService.submitReview(userId, reviewDTO);
        return ResponseEntity.ok("Gửi đánh giá thành công!");
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        // Tạm thời trả về 1L để bạn test khi chưa có Security hoàn chỉnh
        return (principal == null) ? 1L : Long.parseLong(principal.getName());
    }
}