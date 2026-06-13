package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.PromotionRequestDTO;
import org.example.backend.dto.request.WalkInBookingRequestDTO;
import org.example.backend.dto.response.BookingResponseDTO;
import org.example.backend.dto.response.DashboardStatsResponseDTO;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
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
     * Lấy danh sách phòng trống cho form walk-in.
     * Nếu cung cấp checkIn/checkOut, loại trừ các phòng có lịch đặt chồng chéo (kể cả PENDING online).
     * Nếu không có ngày, trả về tất cả phòng có status = AVAILABLE.
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableRoomNumbers(LocalDate checkIn, LocalDate checkOut) {
        List<Room> availableRooms = roomRepository.findAllByStatus(RoomStatus.AVAILABLE);
        if (checkIn == null || checkOut == null) {
            return availableRooms.stream().map(Room::getRoomNumber).collect(Collectors.toList());
        }
        // Bug 3 fix: loại trừ phòng có booking PENDING/CONFIRMED/CHECK_IN trùng khoảng thời gian
        return availableRooms.stream()
                .filter(room -> !bookingRepository.isRoomOccupied(room.getId(), checkIn, checkOut))
                .map(Room::getRoomNumber)
                .collect(Collectors.toList());
    }

    /**
     * Nghiệp vụ Khởi tạo đơn thuê phòng trực tiếp tại quầy Lễ tân (Walk-in)
     *
     * Fix Bug 1: Thêm isRoomOccupied — kiểm tra lịch đặt phòng chồng chéo (bao gồm cả đơn PENDING online)
     * Fix Bug 2: Dùng findByRoomNumberWithLock — Pessimistic Lock chống race condition 2 lễ tân đặt đồng thời
     * Fix Bug 5: Guard endDate == null khi validate coupon — tránh NullPointerException
     * Fix Bug 6: amountPaid = ZERO khi UNPAID — sửa mâu thuẫn dữ liệu "có tiền nhưng chưa thanh toán"
     */
    @Transactional
    public void createWalkInBooking(WalkInBookingRequestDTO request) {
        // Bug 2 fix: Pessimistic Lock — chặn 2 lễ tân đặt cùng phòng đồng thời
        Room room = roomRepository.findByRoomNumberWithLock(request.getRoomNumber())
                .orElseThrow(() -> new RuntimeException("Số phòng " + request.getRoomNumber() + " không tồn tại!"));

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new RuntimeException("Phòng số " + request.getRoomNumber() + " hiện tại không sẵn sàng đón khách!");
        }

        // Bug 1 fix: Kiểm tra lịch đặt chồng chéo — bắt cả đơn PENDING/CONFIRMED/CHECK_IN của online
        boolean occupied = bookingRepository.isRoomOccupied(
                room.getId(), request.getCheckInDate(), request.getCheckOutDate());
        if (occupied) {
            throw new RuntimeException("Phòng " + request.getRoomNumber()
                    + " đã có lịch đặt trong khoảng " + request.getCheckInDate()
                    + " → " + request.getCheckOutDate() + ". Vui lòng kiểm tra lại!");
        }

        // Tính số đêm lưu trú thực tế
        long totalNights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (totalNights <= 0) totalNights = 1;

        BigDecimal basePricePerNight = room.getRoomType().getBasePrice();
        BigDecimal originalTotalPrice = basePricePerNight.multiply(BigDecimal.valueOf(totalNights));

        // Bug 5 fix: Guard null trước khi gọi isBefore/isAfter trên endDate (tránh NPE)
        BigDecimal discountAmount = BigDecimal.ZERO;
        Promotion promotion = null;
        if (request.getAppliedCode() != null && !request.getAppliedCode().trim().isEmpty()) {
            promotion = promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue(request.getAppliedCode().trim()).orElse(null);
            if (promotion != null) {
                boolean validStart = promotion.getStartDate() == null
                        || !request.getCheckInDate().isBefore(promotion.getStartDate());
                boolean validEnd = promotion.getEndDate() == null
                        || !request.getCheckInDate().isAfter(promotion.getEndDate());
                if (validStart && validEnd) {
                    BigDecimal percent = BigDecimal.valueOf(promotion.getDiscountPercentage());
                    discountAmount = originalTotalPrice
                            .multiply(percent)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
            }
        }
        BigDecimal finalAmount = originalTotalPrice.subtract(discountAmount);

        // Khởi tạo thực thể Booking — lưu thông tin khách quầy trực tiếp, không cần tài khoản
        Booking booking = Booking.builder()
                .user(null)
                .customerName(request.getCustomerName().trim())
                .customerPhone(request.getCustomerPhone().trim())
                .customerCccd(request.getCustomerCccd().trim())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalPrice(originalTotalPrice)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .promotion(promotion)
                .status(BookingStatus.CHECK_IN)
                .hotel(room.getHotel())
                .build();
        booking = bookingRepository.save(booking);

        // Snapshot giá phòng tại thời điểm đặt
        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoom(room);
        detail.setPriceAtBooking(basePricePerNight);
        bookingDetailRepository.save(detail);

        // Chuyển trạng thái phòng sang OCCUPIED ngay (khách walk-in đã có mặt)
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        // Bug 6 fix: amountPaid phải là ZERO khi status UNPAID — khách trả khi checkout
        // (amountPaid sẽ được điền đúng tại bước checkOut → processCheckOut)
        Invoice invoice = Invoice.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.CASH)
                .amountPaid(BigDecimal.ZERO)
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

        long totalNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (totalNights <= 0) totalNights = 1;

        BigDecimal basePricePerNight = room.getRoomType().getBasePrice();
        BigDecimal originalTotalPrice = basePricePerNight.multiply(BigDecimal.valueOf(totalNights));

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

        return Map.of(
                "originalPrice", originalTotalPrice,
                "discountAmount", discountAmount,
                "finalAmount", finalAmount
        );
    }

    // =========================================================================
    // 📊 3. PHÂN HỆ THỐNG KÊ DOANH THU & HIỆU SUẤT DASHBOARD (DOCKING FILTER)
    // =========================================================================

    /**
     * 📊 NGHIỆP VỤ NÂNG CẤP: Tổng hợp số liệu báo cáo doanh thu động theo NGÀY / TUẦN / THÁNG / NĂM
     * @param filterType Nhận các giá trị cấu hình: "DATE", "WEEK", "MONTH", "YEAR"
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponseDTO getDashboardStatsFiltered(String filterType) {
        LocalDate today = LocalDate.now();
        List<Invoice> allInvoices = invoiceRepository.findAll();

        // 1. Tính tổng doanh thu thực tế đã thanh toán trong ngày hôm nay
        BigDecimal revenueToday = allInvoices.stream()
                .filter(inv -> inv.getPaymentDate() != null && inv.getPaymentDate().toLocalDate().isEqual(today))
                .filter(inv -> inv.getPaymentStatus() == PaymentStatus.PAID)
                .map(Invoice::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Đếm số lượng đơn đặt phòng phát sinh trong ngày hôm nay
        long bookingsToday = bookingRepository.findAll().stream()
                .filter(b -> b.getCheckInDate().isEqual(today))
                .count();

        // 3. Thống kê trạng thái phòng thời gian thực
        long totalRooms = roomRepository.count();
        long availableRooms = roomRepository.findAllByStatus(RoomStatus.AVAILABLE).size();

        // 4. Phân tách và cấu trúc Map dữ liệu biểu đồ dựa trên bộ lọc thời gian chọn
        Map<String, BigDecimal> chartData = new LinkedHashMap<>();
        String type = (filterType != null) ? filterType.toUpperCase() : "WEEK";

        switch (type) {
            case "DATE":
                // Lọc biểu đồ chi tiết: Hiển thị doanh thu 3 ngày gần đây để đối chiếu sát sườn
                for (int i = 2; i >= 0; i--) {
                    LocalDate targetDate = today.minusDays(i);
                    String label = targetDate.format(DateTimeFormatter.ofPattern("dd/MM"));
                    chartData.put(label, calculateDailyRevenue(allInvoices, targetDate));
                }
                break;

            case "WEEK":
                // Xu hướng thanh khoản trong 7 ngày gần nhất (Mặc định)
                for (int i = 6; i >= 0; i--) {
                    LocalDate targetDate = today.minusDays(i);
                    String label = targetDate.format(DateTimeFormatter.ofPattern("dd/MM"));
                    chartData.put(label, calculateDailyRevenue(allInvoices, targetDate));
                }
                break;

            case "MONTH":
                // Phân rã doanh thu theo 4 tuần trong tháng hiện tại
                for (int i = 3; i >= 0; i--) {
                    LocalDate endWeek = today.minusWeeks(i);
                    LocalDate startWeek = endWeek.minusDays(6);
                    String label = "Tuần " + (4 - i);
                    chartData.put(label, calculatePeriodRevenue(allInvoices, startWeek, endWeek));
                }
                break;

            case "YEAR":
                // Tổng hợp báo cáo lũy kế theo 12 tháng trong năm
                for (int i = 11; i >= 0; i--) {
                    LocalDate targetMonth = today.minusMonths(i);
                    String label = "Th. " + targetMonth.getMonthValue();

                    BigDecimal monthlyRev = allInvoices.stream()
                            .filter(inv -> inv.getPaymentDate() != null
                                    && inv.getPaymentDate().getYear() == targetMonth.getYear()
                                    && inv.getPaymentDate().getMonth() == targetMonth.getMonth())
                            .filter(inv -> inv.getPaymentStatus() == PaymentStatus.PAID)
                            .map(Invoice::getAmountPaid)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    chartData.put(label, monthlyRev);
                }
                break;

            default:
                // Fallback về dữ liệu tuần nếu truyền sai tham số
                for (int i = 6; i >= 0; i--) {
                    LocalDate targetDate = today.minusDays(i);
                    chartData.put(targetDate.toString(), calculateDailyRevenue(allInvoices, targetDate));
                }
                break;
        }

        return DashboardStatsResponseDTO.builder()
                .totalRevenueToday(revenueToday)
                .totalBookingsToday(bookingsToday)
                .availableRooms(availableRooms)
                .totalRooms(totalRooms)
                .last7DaysRevenue(chartData) // Tái tận dụng trường dữ liệu Map để đẩy cấu trúc động về UI
                .build();
    }

    /**
     * Hàm bổ trợ: Tính doanh thu một ngày cụ thể từ danh sách cache
     */
    private BigDecimal calculateDailyRevenue(List<Invoice> invoices, LocalDate date) {
        return invoices.stream()
                .filter(inv -> inv.getPaymentDate() != null && inv.getPaymentDate().toLocalDate().isEqual(date))
                .filter(inv -> inv.getPaymentStatus() == PaymentStatus.PAID)
                .map(Invoice::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Hàm bổ trợ: Tính doanh thu trong khoảng thời gian (Phục vụ phân chia Tuần)
     */
    private BigDecimal calculatePeriodRevenue(List<Invoice> invoices, LocalDate start, LocalDate end) {
        return invoices.stream()
                .filter(inv -> inv.getPaymentDate() != null
                        && !inv.getPaymentDate().toLocalDate().isBefore(start)
                        && !inv.getPaymentDate().toLocalDate().isAfter(end))
                .filter(inv -> inv.getPaymentStatus() == PaymentStatus.PAID)
                .map(Invoice::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}