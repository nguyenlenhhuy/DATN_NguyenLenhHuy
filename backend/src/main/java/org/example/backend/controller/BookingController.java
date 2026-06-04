package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.BookingRequest;
import org.example.backend.dto.request.WalkInBookingRequestDTO;
import org.example.backend.dto.response.BookingHistoryResponseDTO;
import org.example.backend.dto.response.BookingResponseDTO;
import org.example.backend.dto.response.DashboardStatsResponseDTO;
import org.example.backend.dto.response.PromotionResponseDTO;
import org.example.backend.entity.Booking;
import org.example.backend.entity.User;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.UserRepository;
import org.example.backend.service.BookingService;
import org.example.backend.service.ManagementService;
import org.example.backend.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin("*")
public class BookingController {

    private final BookingService bookingService;
    private final ManagementService managementService;
    private final PaymentService paymentService;

    // ✅ TIÊM THÊM USER REPOSITORY ĐỂ LẤY THÔNG TIN TỪ TOKEN
    private final UserRepository userRepository;

    // =========================================================================
    // 🔀 PHÂN HỆ ĐÃ ĐIỀU CHỈNH: ĐỒNG BỘ URL VỚI PHÍA FRONTEND
    // =========================================================================

    @GetMapping("/management/bookings")
    public ResponseEntity<List<BookingResponseDTO>> getManagementBookings() {
        return ResponseEntity.ok(managementService.getAllBookings());
    }

    @GetMapping("/management/bookings/available-rooms")
    public ResponseEntity<List<String>> getAvailableRooms() {
        return ResponseEntity.ok(managementService.getAvailableRoomNumbers());
    }

    @PostMapping("/management/bookings/walk-in")
    public ResponseEntity<Map<String, String>> createWalkInBooking(@RequestBody WalkInBookingRequestDTO requestDTO) {
        managementService.createWalkInBooking(requestDTO);
        return ResponseEntity.ok(Map.of("message", "Khởi tạo đơn thuê phòng tại quầy thành công!"));
    }

    @PostMapping("/management/bookings/{id}/check-in")
    public ResponseEntity<Map<String, String>> processAdminCheckIn(@PathVariable Long id) {
        managementService.processCheckIn(id);
        return ResponseEntity.ok(Map.of("message", "Làm thủ tục nhận phòng hoàn tất!"));
    }

    @PostMapping("/management/bookings/{id}/check-out")
    public ResponseEntity<Map<String, String>> processAdminCheckOut(@PathVariable Long id) {
        managementService.processCheckOut(id);
        return ResponseEntity.ok(Map.of("message", "Làm thủ tục trả phòng và chốt doanh thu thành công!"));
    }

    // =========================================================================
    // 👤 PHÂN HỆ KHÁCH HÀNG (ĐÃ TÍCH HỢP PAYOS & BẢO MẬT TOKEN)
    // =========================================================================

    /**
     * Tạo đơn đặt phòng trực tuyến & Khởi tạo Link thanh toán PayOS
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBooking(
            @Valid @RequestBody BookingRequest request,
            Principal principal) throws Exception {

        // 1. LẤY ID THẬT TỪ TOKEN BẢO MẬT (Cách 2)
        String username = principal.getName();

        // Lưu ý: Sửa 'findByUsername' thành 'findByEmail' nếu hệ thống của bạn đăng nhập bằng Email
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại hoặc phiên đăng nhập đã hết hạn"));

        // Ép cứng ID của người dùng vào request để đảm bảo an toàn
        request.setUserId(currentUser.getId());

        // 2. Tạo đơn đặt phòng
        Booking booking = bookingService.processBooking(request, "ONLINE");

        // 3. Chuẩn bị Response trả về cho Angular
        Map<String, Object> response = new HashMap<>();
        response.put("bookingId", booking.getId());
        response.put("status", booking.getStatus());
        response.put("message", "Đặt phòng thành công!");

        // 4. Nếu khách chọn PAYOS, tạo link thanh toán
        if ("PAYOS".equalsIgnoreCase(request.getPaymentMethod())) {
            Long invoiceId = booking.getInvoice().getId();
            String checkoutUrl = paymentService.createPaymentLink(invoiceId);
            response.put("checkoutUrl", checkoutUrl);
        }

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =========================================================================
    // 🔧 CÁC HÀM XỬ LÝ TRẠNG THÁI & LỊCH SỬ
    // =========================================================================

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
        // Đồng bộ logic trích xuất ID từ Token cho Lịch sử
        String username = principal.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));

        return ResponseEntity.ok(bookingService.getBookingHistory(currentUser.getId()));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<BookingHistoryResponseDTO>> getUserHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }

    @GetMapping("/management/bookings/available-promotions")
    public ResponseEntity<List<PromotionResponseDTO>> getAvailablePromotions() {
        return ResponseEntity.ok(managementService.getAvailablePromotionsForWalkIn());
    }

    @GetMapping("/management/bookings/preview-price")
    public ResponseEntity<Map<String, Object>> previewPrice(
            @RequestParam String roomNumber,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate,
            @RequestParam(required = false) String appliedCode) {

        java.time.LocalDate checkIn = java.time.LocalDate.parse(checkInDate);
        java.time.LocalDate checkOut = java.time.LocalDate.parse(checkOutDate);

        return ResponseEntity.ok(managementService.previewWalkInPrice(roomNumber, checkIn, checkOut, appliedCode));
    }

    @GetMapping("/management/dashboard-stats")
    public ResponseEntity<DashboardStatsResponseDTO> getDashboardStats(
            @RequestParam(defaultValue = "WEEK") String filterType) {
        return ResponseEntity.ok(managementService.getDashboardStatsFiltered(filterType));
    }
    // Lấy lịch sử đặt phòng của khách hàng đang đăng nhập
    @GetMapping("/customer/history")
    public ResponseEntity<List<BookingHistoryResponseDTO>> getCustomerBookingHistory(Principal principal) {
        // 1. Trích xuất username an toàn từ Token bảo mật đã đi qua Interceptor
        String username = principal.getName();

        // 2. Tìm thông tin User dưới DB MySQL
        User currentUser = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new org.example.backend.exception.ResourceNotFoundException("Phiên đăng nhập không hợp lệ hoặc tài khoản đã bị xóa"));

        // 3. Map sang DTO phẳng sạch lỗi tuần hoàn dữ liệu Jackson
        List<BookingHistoryResponseDTO> history = bookingService.getBookingHistory(currentUser.getId());

        return ResponseEntity.ok(history);
    }
}
