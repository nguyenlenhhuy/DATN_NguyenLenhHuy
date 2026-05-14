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
     * Đặt phòng mới
     */
    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequest request) {
        // Clean Code: Không try-catch. Service throw lỗi thì GlobalExceptionHandler sẽ xử lý.
        return new ResponseEntity<>(bookingService.processBooking(request), HttpStatus.CREATED);
    }

    /**
     * Lấy lịch sử đặt phòng của chính người dùng hiện tại (Dựa trên Token)
     */
    @GetMapping("/history")
    public ResponseEntity<List<BookingHistoryResponseDTO>> getMyHistory(Principal principal) {
        // Lưu ý: Đảm bảo JwtProvider của bạn set userId vào subject của Principal
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }

    /**
     * Lấy lịch sử của bất kỳ User nào (Dành cho Admin/Staff)
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<BookingHistoryResponseDTO>> getUserHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }

    /**
     * Hủy đặt phòng
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.ok(Map.of("message", "Đã hủy đặt phòng thành công."));
    }

    /**
     * Xác nhận thanh toán
     */
    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<Map<String, String>> confirmPayment(@PathVariable Long id) {
        bookingService.confirmPayment(id);
        return ResponseEntity.ok(Map.of("message", "Xác nhận thanh toán thành công."));
    }
}