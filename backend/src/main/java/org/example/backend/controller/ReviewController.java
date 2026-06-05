package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.ReplyRequestDTO;
import org.example.backend.dto.request.ReviewRequestDTO;
import org.example.backend.dto.response.ReviewResponseDTO;
import org.example.backend.service.ReviewService;
import org.example.backend.repository.UserRepository;
import org.example.backend.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    // ================= API CÔNG KHAI (MÀN CHI TIẾT PHÒNG) =================

    /**
     * 🔥 THÊM MỚI: API công khai cho khách xem phòng đọc toàn bộ review thật.
     * Endpoint: GET http://localhost:8080/api/reviews/room/{roomId}
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByRoomId(@PathVariable Long roomId) {
        List<ReviewResponseDTO> reviews = reviewService.getReviewsByRoomId(roomId);
        return ResponseEntity.ok(reviews);
    }

    // ================= DÀNH CHO CUSTOMER =================

    @PostMapping("/submit")
    public ResponseEntity<ReviewResponseDTO> createReview(
            @Valid @RequestBody ReviewRequestDTO reviewDTO,
            Principal principal) {

        String username = principal.getName();
        Long userId = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng khớp với phiên đăng nhập"))
                .getId();

        ReviewResponseDTO response = reviewService.submitReview(userId, reviewDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ================= DÀNH CHO ADMIN / STAFF =================

    @GetMapping("/admin/all")
    public ResponseEntity<Page<ReviewResponseDTO>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(reviewService.findAllForAdmin(page, size));
    }

    @PutMapping("/admin/{id}/reply")
    public ResponseEntity<ReviewResponseDTO> replyReview(
            @PathVariable Long id,
            @Valid @RequestBody ReplyRequestDTO replyDTO) {

        ReviewResponseDTO response = reviewService.replyReview(id, replyDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteReview(
            @PathVariable Long id,
            Principal principal) {

        String username = principal.getName();
        Long adminId = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Admin hợp lệ"))
                .getId();

        reviewService.deleteReviewByAdmin(id, adminId);
        return ResponseEntity.ok("Đã xóa đánh giá và cập nhật nhật ký hệ thống.");
    }
    @PutMapping("/admin/{id}/toggle-visibility")
    public ResponseEntity<ReviewResponseDTO> toggleVisibility(@PathVariable Long id) {
        ReviewResponseDTO response = reviewService.toggleReviewVisibility(id);
        return ResponseEntity.ok(response);
    }
}