package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.PromotionRequestDTO;
import org.example.backend.dto.request.WalkInBookingRequestDTO;
import org.example.backend.dto.response.BookingResponseDTO;
import org.example.backend.dto.response.PromotionResponseDTO;
import org.example.backend.entity.*;
import org.example.backend.entity.enums.BookingStatus;
import org.example.backend.entity.enums.RoomStatus;
import org.example.backend.entity.enums.PaymentMethod;
import org.example.backend.entity.enums.PaymentStatus;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagementService {

    private final BookingRepository bookingRepository;
    private final PromotionRepository promotionRepository;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final BookingService bookingService;

    // =========================================================================
    // 🛎️ 1. PHÂN HỆ NGHIỆP VỤ ĐẶT PHÒNG TẠI QUẦY (BOOKINGS MANAGEMENT)
    // =========================================================================

    /**
     * Lấy danh sách toàn bộ đơn đặt phòng đổ ra bảng dữ liệu Angular
     */
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAllBookingsWithInvoiceAndUser().stream().map(b -> {
            String roomNum = "N/A";
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(b.getId());
            if (details != null && !details.isEmpty() && details.get(0).getRoom() != null) {
                roomNum = details.get(0).getRoom().getRoomNumber();
            }

            Invoice inv = b.getInvoice();

            return BookingResponseDTO.builder()
                    .bookingId(b.getId())
                    .roomNumber(roomNum)
                    .customerName(b.getCustomerName() != null ? b.getCustomerName() : (b.getUser() != null ? b.getUser().getFullName() : "Khách vãng lai"))
                    .customerPhone(b.getCustomerPhone() != null ? b.getCustomerPhone() : (b.getUser() != null ? b.getUser().getPhone() : "N/A"))
                    .customerCccd(b.getCustomerCccd())
                    .checkInDate(b.getCheckInDate())
                    .checkOutDate(b.getCheckOutDate())
                    .originalPrice(b.getTotalPrice())
                    .discountAmount(b.getDiscountAmount())
                    .finalAmount(b.getFinalAmount())
                    .appliedCode(b.getPromotion() != null ? b.getPromotion().getCode() : null)
                    .paymentStatus(inv != null ? inv.getPaymentStatus().name() : "UNPAID")
                    .paymentMethod(inv != null ? inv.getPaymentMethod().name() : "CASH")
                    .bookingStatus(b.getStatus().name())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * API Gợi ý: Lấy danh sách toàn bộ Số phòng đang trống (AVAILABLE) thời gian thực
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableRoomNumbers() {
        return roomRepository.findAllByStatus(RoomStatus.AVAILABLE).stream()
                .map(Room::getRoomNumber)
                .collect(Collectors.toList());
    }

    /**
     * Nghiệp vụ Khởi tạo đơn thuê phòng trực tiếp tại quầy Lễ tân (Walk-in)
     */
    @Transactional
    public void createWalkInBooking(WalkInBookingRequestDTO request) {
        Room room = roomRepository.findByRoomNumber(request.getRoomNumber())
                .orElseThrow(() -> new RuntimeException("Số phòng " + request.getRoomNumber() + " không tồn tại!"));

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new RuntimeException("Phòng số " + request.getRoomNumber() + " hiện tại không sẵn sàng đón khách!");
        }

        // Tính số đêm lưu trú thực tế
        long totalNights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (totalNights <= 0) totalNights = 1;

        BigDecimal basePricePerNight = room.getRoomType().getBasePrice();
        BigDecimal originalTotalPrice = basePricePerNight.multiply(BigDecimal.valueOf(totalNights));

        // Kiểm tra và áp dụng mã voucher chiết khấu tiền phòng
        BigDecimal discountAmount = BigDecimal.ZERO;
        Promotion promotion = null;
        if (request.getAppliedCode() != null && !request.getAppliedCode().trim().isEmpty()) {
            promotion = promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue(request.getAppliedCode().trim()).orElse(null);
            if (promotion != null) {
                if (!request.getCheckInDate().isBefore(promotion.getStartDate()) && !request.getCheckInDate().isAfter(promotion.getEndDate())) {
                    BigDecimal percent = BigDecimal.valueOf(promotion.getDiscountPercentage());
                    discountAmount = originalTotalPrice.multiply(percent).divide(BigDecimal.valueOf(100));
                }
            }
        }
        BigDecimal finalAmount = originalTotalPrice.subtract(discountAmount);

        // Khởi tạo thực thể Booking (Lưu thông tin định danh trực tiếp, không bắt buộc user_id)
        Booking booking = Booking.builder()
                .user(null)
                .customerName(request.getCustomerName().trim())
                .customerPhone(request.getCustomerPhone().trim())
                .customerCccd(request.getCustomerCccd().trim()) // Lưu vết thông tin công an yêu cầu
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalPrice(originalTotalPrice)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .promotion(promotion)
                .status(BookingStatus.CHECK_IN) // Khách đặt quầy sẽ trực tiếp nhận phòng và ở luôn
                .hotel(room.getHotel()) // Thừa hưởng ID khách sạn từ phòng vật lý
                .build();
        booking = bookingRepository.save(booking);

        // Ghi nhận Snapshot bảng giá chi tiết đơn phòng
        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoom(room);
        detail.setPriceAtBooking(basePricePerNight);
        bookingDetailRepository.save(detail);

        // Chuyển đổi trạng thái vật lý của phòng sang Đang có người ở
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        // Phát hành hóa đơn Tiền mặt mặc định ở quầy trạng thái chờ thanh toán lúc trả phòng
        Invoice invoice = Invoice.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.CASH)
                .amountPaid(finalAmount)
                .paymentStatus(PaymentStatus.UNPAID)
                .createdAt(LocalDateTime.now())
                .build();
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void processCheckIn(Long bookingId) {
        bookingService.checkIn(bookingId, 1L);
    }

    @Transactional
    public void processCheckOut(Long bookingId) {
        bookingService.checkOut(bookingId, 1L);
    }

    // =========================================================================
    // 🏷️ 2. PHÂN HỆ QUẢN LÝ CHƯƠNG TRÌNH KHUYẾN MÃI (PROMOTIONS)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getAllPromotions() {
        return promotionRepository.findAll().stream().map(p -> PromotionResponseDTO.builder()
                .id(p.getId())
                .code(p.getCode())
                .discountPercentage(p.getDiscountPercentage())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .isActive(p.getIsActive())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public void createPromotion(PromotionRequestDTO request) {
        Promotion promotion = Promotion.builder()
                .code(request.getCode().toUpperCase().trim())
                .discountPercentage(request.getDiscountPercentage())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true)
                .build();
        promotionRepository.save(promotion);
    }

    @Transactional
    public void togglePromotionStatus(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chương trình khuyến mãi không tồn tại!"));
        promotion.setIsActive(!promotion.getIsActive());
        promotionRepository.save(promotion);
    }
    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getAvailablePromotionsForWalkIn() {
        return promotionRepository.findAvailablePromotions(LocalDate.now()).stream().map(p ->
                PromotionResponseDTO.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .discountPercentage(p.getDiscountPercentage())
                        .startDate(p.getStartDate())
                        .endDate(p.getEndDate())
                        .isActive(p.getIsActive())
                        .build()
        ).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public Map<String, Object> previewWalkInPrice(String roomNumber, LocalDate checkIn, LocalDate checkOut, String appliedCode) {
        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại!"));

        // 1. Tính số đêm ở
        long totalNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (totalNights <= 0) totalNights = 1;

        // 2. Lấy giá gốc từ Loại phòng thực tế trong Database của bạn
        BigDecimal basePricePerNight = room.getRoomType().getBasePrice();
        BigDecimal originalTotalPrice = basePricePerNight.multiply(BigDecimal.valueOf(totalNights));

        // 3. Tính toán giảm giá từ Voucher nếu có
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (appliedCode != null && !appliedCode.trim().isEmpty()) {
            Promotion promotion = promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue(appliedCode.trim()).orElse(null);
            if (promotion != null) {
                if (!checkIn.isBefore(promotion.getStartDate()) && !checkIn.isAfter(promotion.getEndDate())) {
                    BigDecimal percent = BigDecimal.valueOf(promotion.getDiscountPercentage());
                    discountAmount = originalTotalPrice.multiply(percent).divide(BigDecimal.valueOf(100));
                }
            }
        }
        BigDecimal finalAmount = originalTotalPrice.subtract(discountAmount);

        // Trả về map dữ liệu cho Controller
        return Map.of(
                "originalPrice", originalTotalPrice,
                "discountAmount", discountAmount,
                "finalAmount", finalAmount
        );
    }
}