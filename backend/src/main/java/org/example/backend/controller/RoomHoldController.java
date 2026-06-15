package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import org.example.backend.service.RoomHoldService;
import org.example.backend.service.RoomHoldService.HoldData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

/**
 * REST API quản lý tạm giữ phòng (Room Hold).
 *
 * POST   /api/rooms/{roomId}/hold          → chiếm giữ phòng (trả về token)
 * PUT    /api/rooms/{roomId}/hold/renew    → gia hạn hold
 * DELETE /api/rooms/{roomId}/hold          → giải phóng hold
 * GET    /api/rooms/{roomId}/hold-status   → kiểm tra trạng thái hold (public)
 * GET    /api/rooms/held                   → danh sách roomId đang bị giữ (public)
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class RoomHoldController {

    private final RoomHoldService holdService;
    private final UserRepository userRepository;

    /** Chiếm giữ phòng — yêu cầu đăng nhập */
    @PostMapping("/{roomId}/hold")
    public ResponseEntity<Map<String, Object>> acquireHold(
            @PathVariable Long roomId,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Vui lòng đăng nhập để đặt phòng."));
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng."));

        String token = holdService.acquireHold(roomId, user.getId(), user.getUsername());
        long remainingSecs = holdService.getHold(roomId)
                .map(HoldData::remainingSeconds).orElse(0L);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "holdToken", token,
                "expiresInSeconds", remainingSecs,
                "message", "Phòng đã được giữ chỗ trong 10 phút."
        ));
    }

    /** Gia hạn hold — dùng khi người dùng vẫn đang trên trang */
    @PutMapping("/{roomId}/hold/renew")
    public ResponseEntity<Map<String, Object>> renewHold(
            @PathVariable Long roomId,
            @RequestParam String holdToken) {

        boolean renewed = holdService.renewHold(roomId, holdToken);
        if (!renewed) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Hold đã hết hạn hoặc token không hợp lệ."));
        }

        long remainingSecs = holdService.getHold(roomId)
                .map(HoldData::remainingSeconds).orElse(0L);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "expiresInSeconds", remainingSecs
        ));
    }

    /** Giải phóng hold — khi người dùng rời trang hoặc đặt phòng xong */
    @DeleteMapping("/{roomId}/hold")
    public ResponseEntity<Void> releaseHold(
            @PathVariable Long roomId,
            @RequestParam(required = false) String holdToken) {

        holdService.releaseHold(roomId, holdToken);
        return ResponseEntity.noContent().build();
    }

    /** Trạng thái hold của một phòng — public endpoint */
    @GetMapping("/{roomId}/hold-status")
    public ResponseEntity<Map<String, Object>> getHoldStatus(@PathVariable Long roomId) {
        return holdService.getHold(roomId)
                .map(h -> ResponseEntity.ok(Map.<String, Object>of(
                        "isHeld", true,
                        "expiresInSeconds", h.remainingSeconds()
                )))
                .orElseGet(() -> ResponseEntity.ok(Map.of("isHeld", false)));
    }

    /** Tất cả roomId đang bị giữ — dùng để render badge trên danh sách phòng */
    @GetMapping("/held")
    public ResponseEntity<Set<Long>> getHeldRoomIds() {
        return ResponseEntity.ok(holdService.getHeldRoomIds());
    }
}
