package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.BookingRequest;
import org.example.backend.dto.response.BookingHistoryResponseDTO;
import org.example.backend.entity.Booking;
import org.example.backend.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin("*")
public class BookingController {

    private final BookingService bookingService;

    /**
     * 1. Đặt phòng mới (Sửa lỗi: Thêm tham số bookingSource)
     */
    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequest request) {
        // Mặc định là ONLINE khi khách đặt qua API này
        return new ResponseEntity<>(bookingService.processBooking(request, "ONLINE"), HttpStatus.CREATED);
    }

    /**
     * 2. Xác nhận thanh toán (Sửa lỗi: Thêm operatorId)
     */
    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<Map<String, String>> confirmPayment(@PathVariable Long id, @RequestParam Long operatorId) {
        bookingService.confirmPayment(id, operatorId);
        return ResponseEntity.ok(Map.of("message", "Xác nhận thanh toán thành công."));
    }

    /**
     * 3. Hủy đặt phòng (Sửa lỗi: Đồng bộ 4 tham số)
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelBooking(
            @PathVariable Long id,
            @RequestParam Long operatorId,
            @RequestParam String userRole,
            @RequestParam(required = false) String reason) {

        bookingService.cancelBooking(id, operatorId, userRole, reason);
        return ResponseEntity.ok(Map.of("message", "Đã hủy đặt phòng thành công."));
    }

    /**
     * 4. Thủ tục Nhận phòng - CHECK-IN (Bổ sung mới)
     */
    @PutMapping("/{id}/check-in")
    public ResponseEntity<Map<String, String>> checkIn(@PathVariable Long id, @RequestParam Long staffId) {
        bookingService.checkIn(id, staffId);
        return ResponseEntity.ok(Map.of("message", "Check-in thành công."));
    }

    /**
     * 5. Thủ tục Trả phòng - CHECK-OUT (Bổ sung mới)
     */
    @PutMapping("/{id}/check-out")
    public ResponseEntity<Map<String, String>> checkOut(@PathVariable Long id, @RequestParam Long staffId) {
        bookingService.checkOut(id, staffId);
        return ResponseEntity.ok(Map.of("message", "Check-out thành công."));
    }

    /**
     * 6. Lấy lịch sử đặt phòng của chính người dùng
     */
    @GetMapping("/history")
    public ResponseEntity<List<BookingHistoryResponseDTO>> getMyHistory(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }

    /**
     * 7. Lấy lịch sử của bất kỳ User nào (Dành cho Admin/Staff)
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<BookingHistoryResponseDTO>> getUserHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }
}