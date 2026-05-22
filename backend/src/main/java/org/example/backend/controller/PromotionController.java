package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.PromotionRequestDTO;
import org.example.backend.dto.response.PromotionResponseDTO;
import org.example.backend.service.PromotionService; // <--- Đổi import sang Service mới này
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings/management/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PromotionController {

    private final PromotionService promotionService; // <--- Tiêm chuẩn Service chuyên biệt Khuyến mãi

    @GetMapping
    public ResponseEntity<List<PromotionResponseDTO>> getPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createPromotion(@RequestBody PromotionRequestDTO requestDTO) {
        promotionService.createPromotion(requestDTO);
        return ResponseEntity.ok(Map.of("message", "Phát hành mã giảm giá mới lên hệ thống thành công!"));
    }
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, String>> togglePromotionStatus(@PathVariable Long id) {
        promotionService.togglePromotionStatus(id);
        return ResponseEntity.ok(Map.of("message", "Thay đổi trạng thái mã khuyến mãi thành công!"));
    }
}
