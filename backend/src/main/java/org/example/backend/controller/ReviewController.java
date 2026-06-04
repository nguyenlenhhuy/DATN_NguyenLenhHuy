package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ReplyRequestDTO;
import org.example.backend.dto.request.ReviewRequestDTO;
import org.example.backend.dto.response.ReviewResponseDTO;
import org.example.backend.service.ReviewService;
import org.example.backend.repository.UserRepository; // Import UserRepository của bạn
import org.example.backend.exception.ResourceNotFoundException;
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
    private final UserRepository userRepository; // Tiêm UserRepository để truy vấn User

    // ================= DÀNH CHO CUSTOMER =================

    /**
     * Khách hàng gửi đánh giá.
     */
    @PostMapping("/submit")
    public ResponseEntity<ReviewResponseDTO> createReview(
            @Valid @RequestBody ReviewRequestDTO reviewDTO,
            Principal principal) {

        // Lấy username từ Token và truy vấn ra ID thật trong Database
        String username = principal.getName();
        Long userId = userRepository.findByUsername(username) // Lưu ý: Đổi thành findByEmail nếu hệ thống bạn dùng email
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng"))
                .getId();

        ReviewResponseDTO response = reviewService.submitReview(userId, reviewDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ================= DÀNH CHO ADMIN =================

    /**
     * Admin lấy danh sách tất cả đánh giá.
     */
    @GetMapping("/admin/all")
    public ResponseEntity<Page<ReviewResponseDTO>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(reviewService.findAllForAdmin(page, size));
    }

    /**
     * Admin/Staff phản hồi đánh giá khách hàng.
     */
    @PutMapping("/admin/{id}/reply")
    public ResponseEntity<ReviewResponseDTO> replyReview(
            @PathVariable Long id,
            @Valid @RequestBody ReplyRequestDTO replyDTO) {

        ReviewResponseDTO response = reviewService.replyReview(id, replyDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin xóa đánh giá vi phạm.
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteReview(
            @PathVariable Long id,
            Principal principal) {

        // Lấy ID thật của Admin để lưu vào Audit Log
        String username = principal.getName();
        Long adminId = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Admin"))
                .getId();

        reviewService.deleteReviewByAdmin(id, adminId);
        return ResponseEntity.ok("Đã xóa đánh giá và cập nhật nhật ký hệ thống.");
    }
}