package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.BookingRequest;
import org.example.backend.dto.request.WalkInBookingRequestDTO;
import org.example.backend.dto.response.BookingHistoryResponseDTO;
import org.example.backend.dto.response.BookingResponseDTO;
import org.example.backend.dto.response.PromotionResponseDTO;
import org.example.backend.entity.Booking;
import org.example.backend.service.BookingService;
import org.example.backend.service.ManagementService;
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
    private final ManagementService managementService;

    // =========================================================================
    // 🔀 PHÂN HỆ ĐÃ ĐIỀU CHỈNH: ĐỒNG BỘ URL VỚI PHÍA FRONTEND (SỬA TRIỆT ĐỂ LỖI NO RESOURCE)
    // =========================================================================

    /**
     * API Admin 1: Lấy toàn bộ danh sách đơn đặt phòng phục vụ bảng đối soát Admin
     * URL thực tế: GET http://localhost:8080/api/bookings/management/bookings
     */
    @GetMapping("/management/bookings")
    public ResponseEntity<List<BookingResponseDTO>> getManagementBookings() {
        return ResponseEntity.ok(managementService.getAllBookings());
    }

    /**
     * 🎯 API ADMIN BỔ SUNG: Lấy danh sách số phòng đang trống để hiển thị lên Dropdown gợi ý ở Angular
     * URL thực tế: GET http://localhost:8080/api/bookings/management/bookings/available-rooms
     */
    @GetMapping("/management/bookings/available-rooms")
    public ResponseEntity<List<String>> getAvailableRooms() {
        return ResponseEntity.ok(managementService.getAvailableRoomNumbers());
    }

    /**
     * API Admin 2: Khởi tạo đơn thuê phòng trực tiếp tại quầy (Walk-in)
     * URL thực tế: POST http://localhost:8080/api/bookings/management/bookings/walk-in
     */
    @PostMapping("/management/bookings/walk-in")
    public ResponseEntity<Map<String, String>> createWalkInBooking(@RequestBody WalkInBookingRequestDTO requestDTO) {
        managementService.createWalkInBooking(requestDTO);
        return ResponseEntity.ok(Map.of("message", "Khởi tạo đơn thuê phòng tại quầy thành công!"));
    }

    /**
     * API Admin 3: Lễ tân làm thủ tục nhận phòng nhanh từ Panel quản trị
     * URL thực tế: POST http://localhost:8080/api/bookings/management/bookings/{id}/check-in
     */
    @PostMapping("/management/bookings/{id}/check-in")
    public ResponseEntity<Map<String, String>> processAdminCheckIn(@PathVariable Long id) {
        managementService.processCheckIn(id);
        return ResponseEntity.ok(Map.of("message", "Làm thủ tục nhận phòng hoàn tất!"));
    }

    /**
     * API Admin 4: Lễ tân làm thủ tục trả phòng & quyết toán từ Panel quản trị
     * URL thực tế: POST http://localhost:8080/api/bookings/management/bookings/{id}/check-out
     */
    @PostMapping("/management/bookings/{id}/check-out")
    public ResponseEntity<Map<String, String>> processAdminCheckOut(@PathVariable Long id) {
        managementService.processCheckOut(id);
        return ResponseEntity.ok(Map.of("message", "Làm thủ tục trả phòng và chốt doanh thu thành công!"));
    }

    // =========================================================================
    // 👤 PHÂN HỆ CŨ: GIỮ NGUYÊN VẸN TOÀN BỘ CÁC HÀM CỦA BẠN Ở DƯỚI KHÔNG THAY ĐỔI
    // =========================================================================
    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequest request) {
        return new ResponseEntity<>(bookingService.processBooking(request, "ONLINE"), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<Map<String, String>> confirmPayment(@PathVariable Long id, @RequestParam Long operatorId) {
        bookingService.confirmPayment(id, operatorId);
        return ResponseEntity.ok(Map.of("message", "Xác nhận thanh toán thành công."));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelBooking(@PathVariable Long id, @RequestParam Long operatorId, @RequestParam String userRole, @RequestParam(required = false) String reason) {
        bookingService.cancelBooking(id, operatorId, userRole, reason);
        return ResponseEntity.ok(Map.of("message", "Đã hủy đặt phòng thành công."));
    }

    @PutMapping("/{id}/check-in")
    public ResponseEntity<Map<String, String>> checkIn(@PathVariable Long id, @RequestParam Long staffId) {
        bookingService.checkIn(id, staffId);
        return ResponseEntity.ok(Map.of("message", "Check-in thành công."));
    }

    @PutMapping("/{id}/check-out")
    public ResponseEntity<Map<String, String>> checkOut(@PathVariable Long id, @RequestParam Long staffId) {
        bookingService.checkOut(id, staffId);
        return ResponseEntity.ok(Map.of("message", "Check-out thành công."));
    }

    @GetMapping("/history")
    public ResponseEntity<List<BookingHistoryResponseDTO>> getMyHistory(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<BookingHistoryResponseDTO>> getUserHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }
    @GetMapping("/management/bookings/available-promotions")
    public ResponseEntity<List<PromotionResponseDTO>> getAvailablePromotions() {
        return ResponseEntity.ok(managementService.getAvailablePromotionsForWalkIn());
    }
}