package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ReviewRequestDTO;
import org.example.backend.dto.response.ReviewResponseDTO;
import org.example.backend.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ================= DÀNH CHO CUSTOMER =================

    /**
     * Khách hàng gửi đánh giá.
     * ID người dùng được lấy trực tiếp từ Principal (Token hệ thống).
     */
    @PostMapping("/submit")
    public ResponseEntity<ReviewResponseDTO> createReview(
            @Valid @RequestBody ReviewRequestDTO reviewDTO,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        ReviewResponseDTO response = reviewService.submitReview(userId, reviewDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ================= DÀNH CHO ADMIN =================

    /**
     * Admin lấy danh sách tất cả đánh giá để quản lý nội dung.
     */
    @GetMapping("/admin/all")
    public ResponseEntity<Page<ReviewResponseDTO>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(reviewService.findAllForAdmin(page, size));
    }

    /**
     * Admin phản hồi đánh giá khách hàng.
     */
    @PutMapping("/admin/{id}/reply")
    public ResponseEntity<ReviewResponseDTO> replyReview(
            @PathVariable Long id,
            @RequestBody String content) {

        return ResponseEntity.ok(reviewService.replyReview(id, content));
    }

    /**
     * Admin xóa đánh giá vi phạm.
     * ID Admin được lấy từ Principal để ghi Audit Log chính xác.
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteReview(
            @PathVariable Long id,
            Principal principal) {

        Long adminId = Long.parseLong(principal.getName());
        reviewService.deleteReviewByAdmin(id, adminId);
        return ResponseEntity.ok("Đã xóa đánh giá và cập nhật nhật ký hệ thống.");
    }
}