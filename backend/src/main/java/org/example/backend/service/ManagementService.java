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
import java.util.ArrayList;
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
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(b.getId());

            List<String> roomNums = details != null
                    ? details.stream()
                        .filter(d -> d.getRoom() != null)
                        .map(d -> d.getRoom().getRoomNumber())
                        .collect(Collectors.toList())
                    : java.util.List.of();

            String primaryRoom = roomNums.isEmpty() ? "N/A" : roomNums.get(0);

            Invoice inv = b.getInvoice();

            return BookingResponseDTO.builder()
                    .bookingId(b.getId())
                    .roomNumber(primaryRoom)
                    .roomNumbers(roomNums)
                    .roomCount(roomNums.size())
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
     * Lấy danh sách phòng trống kèm chi tiết (tầng, loại, giá) để hiển thị lưới chọn phòng walk-in.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableRoomsDetail(LocalDate checkIn, LocalDate checkOut) {
        List<Room> availableRooms = roomRepository.findAllByStatus(RoomStatus.AVAILABLE);
        return availableRooms.stream()
                .filter(r -> checkIn == null || checkOut == null
                        || !bookingRepository.isRoomOccupied(r.getId(), checkIn, checkOut))
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("roomNumber", r.getRoomNumber());
                    m.put("floor", r.getFloor());
                    m.put("typeName", r.getRoomType() != null ? r.getRoomType().getTypeName() : "N/A");
                    m.put("price", r.getRoomType() != null ? r.getRoomType().getBasePrice() : BigDecimal.ZERO);
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * Preview giá tạm tính cho nhiều phòng (multi-room walk-in).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> previewWalkInPriceMulti(List<String> roomNumbers, LocalDate checkIn, LocalDate checkOut, String appliedCode) {
        if (checkIn == null || checkOut == null || roomNumbers == null || roomNumbers.isEmpty()) {
            return Map.of("originalPrice", BigDecimal.ZERO, "discountAmount", BigDecimal.ZERO, "finalAmount", BigDecimal.ZERO);
        }
        long totalNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (totalNights <= 0) totalNights = 1;
        final long nights = totalNights;

        BigDecimal originalTotal = BigDecimal.ZERO;
        for (String roomNum : roomNumbers) {
            Room room = roomRepository.findByRoomNumber(roomNum).orElse(null);
            if (room != null && room.getRoomType() != null) {
                originalTotal = originalTotal.add(room.getRoomType().getBasePrice().multiply(BigDecimal.valueOf(nights)));
            }
        }
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (appliedCode != null && !appliedCode.trim().isEmpty()) {
            Promotion promo = promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue(appliedCode.trim()).orElse(null);
            if (promo != null) {
                boolean validStart = promo.getStartDate() == null || !checkIn.isBefore(promo.getStartDate());
                boolean validEnd = promo.getEndDate() == null || !checkIn.isAfter(promo.getEndDate());
                if (validStart && validEnd) {
                    discountAmount = originalTotal.multiply(BigDecimal.valueOf(promo.getDiscountPercentage()))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
            }
        }
        return Map.of("originalPrice", originalTotal, "discountAmount", discountAmount, "finalAmount", originalTotal.subtract(discountAmount));
    }

    /**
     * Khởi tạo đơn thuê phòng tại quầy — hỗ trợ đặt nhiều phòng cùng lúc cho 1 khách.
     *
     * - Dùng roomNumbers (multi) hoặc fallback về roomNumber (single, backward compat)
     * - Pessimistic Lock từng phòng trước khi tạo booking
     * - Tạo 1 Booking + N BookingDetail + 1 Invoice chung
     */
    @Transactional
    public void createWalkInBooking(WalkInBookingRequestDTO request) {
        List<String> roomNums = (request.getRoomNumbers() != null && !request.getRoomNumbers().isEmpty())
                ? request.getRoomNumbers()
                : (request.getRoomNumber() != null && !request.getRoomNumber().isEmpty()
                   ? List.of(request.getRoomNumber()) : List.of());

        if (roomNums.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất 1 phòng!");
        }

        long totalNights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (totalNights <= 0) totalNights = 1;
        final long nights = totalNights;

        // Pessimistic Lock và validate tất cả phòng trước khi ghi DB
        List<Room> rooms = new ArrayList<>();
        for (String roomNum : roomNums) {
            Room room = roomRepository.findByRoomNumberWithLock(roomNum)
                    .orElseThrow(() -> new RuntimeException("Số phòng " + roomNum + " không tồn tại!"));
            if (room.getStatus() != RoomStatus.AVAILABLE) {
                throw new RuntimeException("Phòng số " + roomNum + " hiện tại không sẵn sàng đón khách!");
            }
            boolean occupied = bookingRepository.isRoomOccupied(room.getId(), request.getCheckInDate(), request.getCheckOutDate());
            if (occupied) {
                throw new RuntimeException("Phòng " + roomNum + " đã có lịch đặt trong khoảng "
                        + request.getCheckInDate() + " → " + request.getCheckOutDate() + ". Vui lòng kiểm tra lại!");
            }
            rooms.add(room);
        }

        // Tổng giá gốc = Σ(giá/đêm × số đêm) trên tất cả phòng
        BigDecimal originalTotalPrice = rooms.stream()
                .map(r -> r.getRoomType().getBasePrice().multiply(BigDecimal.valueOf(nights)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
                    discountAmount = originalTotalPrice
                            .multiply(BigDecimal.valueOf(promotion.getDiscountPercentage()))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
            }
        }
        BigDecimal finalAmount = originalTotalPrice.subtract(discountAmount);

        String customerEmailTrimmed = (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank())
                ? request.getCustomerEmail().trim() : null;

        Booking booking = Booking.builder()
                .user(null)
                .customerName(request.getCustomerName().trim())
                .customerPhone(request.getCustomerPhone().trim())
                .customerCccd(request.getCustomerCccd().trim())
                .customerEmail(customerEmailTrimmed)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalPrice(originalTotalPrice)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .promotion(promotion)
                .status(BookingStatus.CHECK_IN)
                .hotel(rooms.get(0).getHotel())
                .build();
        booking = bookingRepository.save(booking);

        // Tạo BookingDetail và chuyển OCCUPIED cho từng phòng
        for (Room room : rooms) {
            BookingDetail detail = new BookingDetail();
            detail.setBooking(booking);
            detail.setRoom(room);
            detail.setPriceAtBooking(room.getRoomType().getBasePrice());
            bookingDetailRepository.save(detail);
            room.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(room);
        }

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
                // Fix Bug 2: Guard null startDate/endDate (đồng bộ với createWalkInBooking)
                boolean validStart = promotion.getStartDate() == null
                        || !checkIn.isBefore(promotion.getStartDate());
                boolean validEnd = promotion.getEndDate() == null
                        || !checkIn.isAfter(promotion.getEndDate());
                if (validStart && validEnd) {
                    BigDecimal percent = BigDecimal.valueOf(promotion.getDiscountPercentage());
                    discountAmount = originalTotalPrice.multiply(percent)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
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

    @Transactional(readOnly = true)
    public DashboardStatsResponseDTO getDashboardStatsFiltered(String filterType) {
        LocalDate today = LocalDate.now();

        // Fix Bug 1: Dùng query COUNT/SUM trực tiếp thay vì load toàn bộ bảng vào RAM
        BigDecimal revenueToday = safeRevenue(invoiceRepository.calculateTotalRevenue(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay()));

        long bookingsToday = bookingRepository.countBookingsCreatedBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        long totalRooms = roomRepository.count();
        // Fix occupancy bug: đếm phòng có booking đang hoạt động hôm nay (CONFIRMED/CHECK_IN, date overlap)
        // countByStatusAndNotDeleted(AVAILABLE) không chính xác vì online booking CONFIRMED giữ phòng ở AVAILABLE
        long occupiedToday = bookingDetailRepository.countOccupiedRoomsOnDay(today);
        long availableRooms = Math.max(totalRooms - occupiedToday, 0);

        Map<String, BigDecimal> chartData = new LinkedHashMap<>();
        BigDecimal periodRevenue = BigDecimal.ZERO;
        String type = (filterType != null) ? filterType.toUpperCase() : "WEEK";

        switch (type) {
            case "DATE":
                // 3 ngày gần nhất để so sánh chi tiết
                for (int i = 2; i >= 0; i--) {
                    LocalDate d = today.minusDays(i);
                    BigDecimal rev = revenueForDay(d);
                    chartData.put(d.format(DateTimeFormatter.ofPattern("dd/MM")), rev);
                    periodRevenue = periodRevenue.add(rev);
                }
                break;

            case "WEEK":
                for (int i = 6; i >= 0; i--) {
                    LocalDate d = today.minusDays(i);
                    BigDecimal rev = revenueForDay(d);
                    chartData.put(d.format(DateTimeFormatter.ofPattern("dd/MM")), rev);
                    periodRevenue = periodRevenue.add(rev);
                }
                break;

            case "MONTH":
                // Fix Bug 3: Tuần theo lịch tháng hiện tại (ngày 1-7, 8-14, 15-21, 22-hết tháng)
                // Đồng bộ với logic frontend filterBookingsByChartDate
                LocalDate monthStart = today.withDayOfMonth(1);
                int daysInMonth = today.lengthOfMonth();
                for (int week = 1; week <= 4; week++) {
                    LocalDate startWeek = monthStart.plusDays((long) (week - 1) * 7);
                    LocalDate endWeek = (week == 4)
                            ? monthStart.plusDays(daysInMonth - 1)
                            : startWeek.plusDays(6);
                    BigDecimal rev = revenueForPeriod(startWeek, endWeek);
                    chartData.put("Tuần " + week, rev);
                    periodRevenue = periodRevenue.add(rev);
                }
                break;

            case "YEAR":
            default:
                // Fix Bug 7: default cũng dùng dd/MM như WEEK, không dùng toString()
                for (int i = 11; i >= 0; i--) {
                    LocalDate m = today.minusMonths(i);
                    LocalDate mStart = m.withDayOfMonth(1);
                    LocalDate mEnd = m.withDayOfMonth(m.lengthOfMonth());
                    BigDecimal rev = revenueForPeriod(mStart, mEnd);
                    chartData.put("Th. " + m.getMonthValue(), rev);
                    periodRevenue = periodRevenue.add(rev);
                }
                break;
        }

        return DashboardStatsResponseDTO.builder()
                .totalRevenueToday(revenueToday)
                .totalRevenuePeriod(periodRevenue)   // Fix Bug 4: KPI card dùng đúng kỳ đang xem
                .totalBookingsToday(bookingsToday)
                .availableRooms(availableRooms)
                .totalRooms(totalRooms)
                .last7DaysRevenue(chartData)
                .build();
    }

    // =========================================================================
    // 📅 4. CÔNG SUẤT PHÒNG THEO NGÀY — CALENDAR HEATMAP
    // =========================================================================

    @Transactional(readOnly = true)
    public Map<String, Integer> getRoomOccupancyCalendar(int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        long totalRooms = Math.max(roomRepository.count(), 1);

        Map<String, Integer> result = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate cursor = monthStart;
        while (!cursor.isAfter(monthEnd)) {
            long occupied = bookingDetailRepository.countOccupiedRoomsOnDay(cursor);
            // Trả % công suất (0–100) để frontend dễ tô màu gradient
            int pct = (int) Math.min(Math.round(occupied * 100.0 / totalRooms), 100);
            result.put(cursor.format(fmt), pct);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    // =========================================================================
    // 📋 5. CHI TIẾT CÔNG SUẤT THEO NGÀY — CLICK CALENDAR CELL
    // =========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getRoomOccupancyDayDetail(LocalDate date) {
        List<BookingStatus> activeStatuses = java.util.List.of(BookingStatus.CONFIRMED, BookingStatus.CHECK_IN, BookingStatus.CHECK_OUT);
        List<BookingDetail> details = bookingDetailRepository.findOccupiedRoomsDetailOnDay(date, activeStatuses);
        long totalRooms = Math.max(roomRepository.count(), 1);

        // Đếm phòng duy nhất đang được thuê (1 booking có thể có nhiều BookingDetail/phòng)
        long occupiedRooms = details.stream()
                .map(bd -> bd.getRoom().getId())
                .distinct()
                .count();
        long availableRooms = Math.max(totalRooms - occupiedRooms, 0);
        int pct = (int) Math.min(Math.round(occupiedRooms * 100.0 / totalRooms), 100);

        long checkInCount   = details.stream().filter(bd -> bd.getBooking().getStatus() == BookingStatus.CHECK_IN).count();
        long confirmedCount = details.stream().filter(bd -> bd.getBooking().getStatus() == BookingStatus.CONFIRMED).count();
        long checkOutCount  = details.stream().filter(bd -> bd.getBooking().getStatus() == BookingStatus.CHECK_OUT).count();

        List<Map<String, Object>> roomList = details.stream().map(bd -> {
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("roomNumber", bd.getRoom().getRoomNumber());
            rm.put("roomType",   bd.getRoom().getRoomType() != null ? bd.getRoom().getRoomType().getTypeName() : "N/A");
            rm.put("bookingStatus", bd.getBooking().getStatus().name());
            String customer = bd.getBooking().getCustomerName();
            if (customer == null && bd.getBooking().getUser() != null) customer = bd.getBooking().getUser().getFullName();
            rm.put("customerName", customer != null ? customer : "Khách vãng lai");
            rm.put("checkInDate",  bd.getBooking().getCheckInDate().toString());
            rm.put("checkOutDate", bd.getBooking().getCheckOutDate().toString());
            return rm;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date",          date.toString());
        result.put("totalRooms",    totalRooms);
        result.put("occupiedRooms", occupiedRooms);
        result.put("availableRooms",availableRooms);
        result.put("occupancyRate", pct);
        result.put("checkInCount",   checkInCount);
        result.put("confirmedCount", confirmedCount);
        result.put("checkOutCount",  checkOutCount);
        result.put("rooms",         roomList);
        return result;
    }

    private BigDecimal revenueForDay(LocalDate date) {
        return safeRevenue(invoiceRepository.calculateTotalRevenue(
                date.atStartOfDay(), date.plusDays(1).atStartOfDay()));
    }

    private BigDecimal revenueForPeriod(LocalDate start, LocalDate end) {
        return safeRevenue(invoiceRepository.calculateTotalRevenue(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay()));
    }

    private BigDecimal safeRevenue(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}