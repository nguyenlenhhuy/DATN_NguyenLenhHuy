package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.PromotionRequestDTO;
import org.example.backend.dto.response.PromotionResponseDTO;
import org.example.backend.service.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings/management/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PromotionController {

    private final PromotionService promotionService;

    // =========================================================================
    // CHỨC NĂNG DÀNH CHO ADMIN / STAFF (QUẢN LÝ)
    // =========================================================================

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

    // =========================================================================
    // CHỨC NĂNG DÀNH CHO CUSTOMER (NÚT ÁP DỤNG TRÊN INTERFACE ĐẶT PHÒNG)
    // =========================================================================

    /**
     * API kiểm tra mã giảm giá và tính toán số tiền được giảm trực tiếp cho khách.
     * URL mẫu: GET /api/bookings/management/promotions/validate?code=MIENPHI100&&amount=1500000
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validatePromotion(
            @RequestParam("code") String code,
            @RequestParam("amount") BigDecimal amount) {

        // Gọi xuống Service chuyên biệt để bóc tách, tính toán số tiền giảm giá
        BigDecimal discountAmount = promotionService.calculateDiscount(code, amount);
        BigDecimal finalAmount = amount.subtract(discountAmount);

        // Trả về một Map chứa thông tin xử lý để Angular cập nhật UI
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "discountAmount", discountAmount,
                "finalAmount", finalAmount
        ));
    }
}