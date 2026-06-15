package org.example.backend.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.BatchBookingRequest;
import org.example.backend.dto.request.BookingRequest;
import org.example.backend.dto.response.BookingHistoryResponseDTO;
import org.example.backend.entity.*;
import org.example.backend.entity.enums.BookingStatus;
import org.example.backend.entity.enums.PaymentMethod;
import org.example.backend.entity.enums.PaymentStatus;
import org.example.backend.entity.enums.RoomStatus;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.entity.DailyRevenueStat;
import org.example.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ReviewRepository reviewRepository;
    private final RoomPriceRepository roomPriceRepository;
    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;
    private final UserRepository userRepository;

    private final DashboardRepository dashboardRepository;

    private final PromotionService promotionService;
    private final EmailService emailService;
    private final RoomHoldService roomHoldService;

    /**
     * 1. CHỨC NĂNG ĐẶT PHÒNG TRỰC TUYẾN
     */
    @Transactional
    public Booking processBooking(BookingRequest request, String bookingSource) {
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng!");
        }

        // Pessimistic Lock — khóa row DB, chặn race condition đặt cùng lúc
        Room room = roomRepository.findByIdWithLock(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại!"));

        // Kiểm tra hold: nếu phòng đang bị người KHÁC giữ → từ chối ngay
        if (request.getUserId() != null
                && roomHoldService.isHeldByOther(room.getId(), request.getUserId())) {
            throw new IllegalStateException(
                    "Phòng đang được nhân viên/khách hàng khác xử lý. Vui lòng thử lại sau ít phút!");
        }

        // Kiểm tra trùng lịch đặt phòng (Chống Overbooking)
        boolean occupied = bookingRepository.isRoomOccupied(
                room.getId(), request.getCheckIn(), request.getCheckOut());

        if (occupied) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phòng này đã có người đặt từ ngày " + request.getCheckIn()
                    + " đến " + request.getCheckOut()
                    + ". Vui lòng chọn khoảng thời gian khác!");
        }

        // Tính toán tổng tiền gốc dựa trên cấu hình giá ngày thường / ngày lễ
        BigDecimal totalPrice = calculateTotalAmount(room, request.getCheckIn(), request.getCheckOut());

        // Mặc định ban đầu khi chưa áp voucher
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = totalPrice;

        // 🔥 XỬ LÝ MÃ GIẢM GIÁ: Kiểm tra nếu request từ Angular gửi lên có kèm Coupon
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            try {
                // Tái tính toán số tiền giảm độc lập dưới Backend để đảm bảo bảo mật
                discountAmount = promotionService.calculateDiscount(request.getCouponCode(), totalPrice);
                finalAmount = totalPrice.subtract(discountAmount);
            } catch (Exception e) {
                // Nếu mã rác hoặc hết hạn đột ngột, ném lỗi chặn đứng hành vi đặt phòng trái phép
                throw new IllegalArgumentException("Mã giảm giá không hợp lệ hoặc đã hết hạn: " + e.getMessage());
            }
        }

        // Khởi tạo thực thể đơn hàng
        Booking booking = new Booking();
        if (request.getUserId() != null) {
            booking.setUser(entityManager.getReference(User.class, request.getUserId()));
        }
        booking.setCheckInDate(request.getCheckIn());
        booking.setCheckOutDate(request.getCheckOut());

        // Đảm bảo điền đầy đủ thông tin tiền tệ đã qua xử lý giảm giá vào Database
        booking.setTotalPrice(totalPrice); // Giá gốc trước giảm
        booking.setDiscountAmount(discountAmount); // Số tiền được giảm
        booking.setFinalAmount(finalAmount); // 🔥 SỐ TIỀN THỰC TẾ KHÁCH PHẢI TRẢ (ĐỒNG BỘ SANG PAYOS)

        // Tự động trích xuất thực thể Hotel từ cấu trúc Room để lưu vào Booking
        if (room.getRoomType() != null && room.getRoomType().getHotel() != null) {
            booking.setHotel(room.getRoomType().getHotel());
        }

        if ("OFFLINE".equalsIgnoreCase(bookingSource)) {
            booking.setStatus(BookingStatus.CONFIRMED);
        } else {
            booking.setStatus(BookingStatus.PENDING);
        }

        // Thực hiện lưu Booking xuống DB trước để sinh ra ID
        Booking savedBooking = bookingRepository.save(booking);

        // Khởi tạo bản ghi chi tiết (BookingDetail) nhằm Snapshot lại giá phòng tại thời điểm đặt
        long totalDays = Duration.between(request.getCheckIn().atStartOfDay(), request.getCheckOut().atStartOfDay()).toDays();
        BigDecimal avgPriceSnapshot = totalPrice.divide(BigDecimal.valueOf(totalDays > 0 ? totalDays : 1), 2, RoundingMode.HALF_UP);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(savedBooking);
        detail.setRoom(room);
        detail.setPriceAtBooking(avgPriceSnapshot);
        bookingDetailRepository.save(detail);

        // Khởi tạo hóa đơn đi kèm (Invoice) - TRUYỀN SỐ TIỀN FINAL_AMOUNT ĐÃ GIẢM SANG
        createInvoice(savedBooking, request.getPaymentMethod(), finalAmount, bookingSource);

        // Gửi email hóa đơn ngay nếu BANK_TRANSFER trực tuyến (invoice đã PAID tức thì)
        if (!"OFFLINE".equalsIgnoreCase(bookingSource) && "BANK_TRANSFER".equalsIgnoreCase(request.getPaymentMethod())) {
            String toEmail = null;
            try { toEmail = savedBooking.getUser() != null ? savedBooking.getUser().getEmail() : null; } catch (Exception ignored) {}
            if (toEmail != null && !toEmail.isBlank()) {
                String rNum = room.getRoomNumber();
                String rType = room.getRoomType() != null ? room.getRoomType().getTypeName() : "N/A";
                String hName = (room.getRoomType() != null && room.getRoomType().getHotel() != null)
                        ? room.getRoomType().getHotel().getName() : "LuxeHotel";
                final String fe = toEmail, fr = rNum, ft = rType, fh = hName;
                final Invoice finalInv = savedBooking.getInvoice();
                final Booking fb = savedBooking;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() {
                        emailService.sendInvoiceEmail(fe, fb, finalInv, fr, ft, fh);
                    }
                });
            }
        }

        // Ghi lại vết hệ thống
        saveAuditLog(request.getUserId(), "CREATE_BOOKING", savedBooking.getId(),
                String.format("Tạo đơn đặt phòng thành công qua %s. Tổng gốc: %s, Thực trả: %s", bookingSource, totalPrice, finalAmount));

        // Giải phóng hold sau khi booking được lưu thành công
        roomHoldService.releaseHold(room.getId(), request.getHoldToken());

        return savedBooking;
    }

    /**
     * 2. XÁC NHẬN THANH TOÁN
     */
    @Transactional
    public void confirmPayment(Long bookingId, Long operatorId) {
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Đơn hàng này không ở trạng thái chờ thanh toán!");
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));

        invoice.setPaymentStatus(PaymentStatus.PAID);
        // 🌟 SỬA ĐỒNG BỘ: Điền số tiền thực trả sau khi giảm vào amountPaid của hóa đơn
        invoice.setAmountPaid(booking.getFinalAmount());
        invoice.setPaymentDate(LocalDateTime.now());

        bookingRepository.save(booking);
        invoiceRepository.save(invoice);

        // Gửi email hóa đơn cho khách sau khi xác nhận thanh toán
        String toEmail = booking.getUser() != null ? booking.getUser().getEmail() : null;
        if (toEmail != null && !toEmail.isBlank()) {
            String roomNumber = "N/A", roomType = "N/A", hotelName = "LuxeHotel";
            List<BookingDetail> details = bookingDetailRepository.findByBookingId(bookingId);
            if (!details.isEmpty()) {
                org.example.backend.entity.Room r = details.get(0).getRoom();
                if (r != null) {
                    roomNumber = r.getRoomNumber();
                    if (r.getRoomType() != null) {
                        roomType = r.getRoomType().getTypeName();
                        if (r.getRoomType().getHotel() != null) hotelName = r.getRoomType().getHotel().getName();
                    }
                }
            }
            final String finalEmail = toEmail, finalRoom = roomNumber, finalType = roomType, finalHotel = hotelName;
            final Invoice finalInvoice = invoice;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.sendInvoiceEmail(finalEmail, booking, finalInvoice, finalRoom, finalType, finalHotel);
                }
            });
        }

        saveAuditLog(operatorId, "CONFIRM_PAYMENT", bookingId, "Xác nhận thanh toán thành công.");
    }

    /**
     * 3. HỦY ĐẶT PHÒNG
     */
    @Transactional
    public void cancelBooking(Long bookingId, Long operatorId, String userRole, String reason) {
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đặt phòng này."));

        if (booking.getStatus() == BookingStatus.CHECK_IN || booking.getStatus() == BookingStatus.CHECK_OUT) {
            if (!"ADMIN".equalsIgnoreCase(userRole)) {
                throw new IllegalStateException("Chỉ Admin mới có quyền hủy đơn đã check-in!");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInDeadline = booking.getCheckInDate().atTime(14, 0);
        long hoursUntilCheckIn = Duration.between(now, checkInDeadline).toHours();

        Invoice invoice = invoiceRepository.findByBookingId(bookingId).orElse(null);
        if (invoice != null && invoice.getPaymentStatus() == PaymentStatus.PAID) {
            if (hoursUntilCheckIn >= 24) {
                invoice.setPaymentStatus(PaymentStatus.REFUNDED);
            }
            invoiceRepository.save(invoice);
        }

        if (booking.getStatus() == BookingStatus.CHECK_IN) {
            roomRepository.updateRoomStatusByBookingId(bookingId, RoomStatus.AVAILABLE);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        saveAuditLog(operatorId, "CANCEL_BOOKING", bookingId, "Lý do: " + reason);
    }

    /**
     * 4. NHẬN PHÒNG (CHECK-IN)
     */
    @Transactional
    public void checkIn(Long bookingId, Long staffId) {
        // 1. Tìm đơn đặt phòng và khóa bản ghi (Pessimistic Lock) tránh xung đột đồng thời
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng với ID: " + bookingId));

        // 2. SỬA LỖI CHẶN: Nới lỏng điều kiện, cho phép các đơn CONFIRMED hoặc PENDING (Chờ nhận phòng ngoài giao diện) được qua
        if (booking.getStatus() == BookingStatus.CHECK_IN || booking.getStatus() == BookingStatus.CHECK_OUT) {
            throw new IllegalStateException("Đơn đặt phòng này đã làm thủ tục nhận/trả phòng trước đó!");
        }

        // 3. SỬA LỖI ENUM: Đổi từ CHECK_IN sang CHECKED_IN để đồng bộ với cấu hình hiển thị Badge màu của Angular
        booking.setStatus(BookingStatus.CHECK_IN);
        bookingRepository.save(booking);

        // 4. Đồng bộ ma trận phòng sang trạng thái OCCUPIED (Có khách ở)
        roomRepository.updateRoomStatusByBookingId(bookingId, RoomStatus.OCCUPIED);

        // 5. Ghi nhật ký hệ thống
        saveAuditLog(staffId, "CHECK_IN", bookingId, "Nhân viên ID " + staffId + " thực hiện thủ tục Check-in thành công cho đơn #" + bookingId);
    }

    /**
     * 5. TRẢ PHÒNG (CHECK-OUT)
     */
    @Transactional
    public void checkOut(Long bookingId, Long staffId) {
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng"));

        if (booking.getStatus() != BookingStatus.CHECK_IN) {
            throw new IllegalStateException("Đơn chưa Check-in, không thể Check-out!");
        }

        booking.setStatus(BookingStatus.CHECK_OUT);
        bookingRepository.save(booking);

        roomRepository.updateRoomStatusByBookingId(bookingId, RoomStatus.DIRTY);

        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));

        if (invoice.getPaymentStatus() != PaymentStatus.PAID) {
            invoice.setPaymentStatus(PaymentStatus.PAID);
            invoice.setAmountPaid(booking.getFinalAmount());
            invoice.setPaymentDate(LocalDateTime.now());
            invoiceRepository.save(invoice);

            // Gửi email hóa đơn sau khi thanh toán tại quầy (checkout)
            String toEmail = null;
            try { toEmail = booking.getUser() != null ? booking.getUser().getEmail() : null; } catch (Exception ignored) {}
            if (toEmail == null && booking.getCustomerEmail() != null && !booking.getCustomerEmail().isBlank()) {
                toEmail = booking.getCustomerEmail();
            }
            if (toEmail != null && !toEmail.isBlank()) {
                String roomNumber = "N/A", roomType = "N/A", hotelName = "LuxeHotel";
                List<BookingDetail> details = bookingDetailRepository.findByBookingId(bookingId);
                if (!details.isEmpty()) {
                    org.example.backend.entity.Room r = details.get(0).getRoom();
                    if (r != null) {
                        roomNumber = r.getRoomNumber();
                        if (r.getRoomType() != null) {
                            roomType = r.getRoomType().getTypeName();
                            if (r.getRoomType().getHotel() != null) hotelName = r.getRoomType().getHotel().getName();
                        }
                    }
                }
                final String fe = toEmail, fr = roomNumber, ft = roomType, fh = hotelName;
                final Invoice finalInv = invoice;
                final Booking fb = booking;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() {
                        emailService.sendInvoiceEmail(fe, fb, finalInv, fr, ft, fh);
                    }
                });
            }
        }

        saveAuditLog(staffId, "CHECK_OUT", bookingId, "Check-out thành công.");
    }

    /**
     * 6. LỊCH SỬ ĐẶT PHÒNG
     */
    /**
     * 6. LỊCH SỬ ĐẶT PHÒNG (BẢN CHUẨN HÓA AN TOÀN TUYỆT ĐỐI)
     */
    public List<BookingHistoryResponseDTO> getBookingHistory(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return bookings.stream().map(booking -> {
            String hotelName = "LuxeHotel";
            String hotelAddress = "Hệ thống trực tuyến";
            // Phòng đầu tiên (fallback/backward-compat)
            Long firstRoomId = null;
            String firstRoomNumber = "N/A";
            String firstRoomType = "N/A";
            List<BookingHistoryResponseDTO.RoomInfo> roomInfoList = new java.util.ArrayList<>();

            try {
                if (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty()) {
                    for (BookingDetail detail : booking.getBookingDetails()) {
                        if (detail.getRoom() == null) continue;
                        Room room = detail.getRoom();
                        String rType = "N/A";
                        if (room.getRoomType() != null) {
                            rType = room.getRoomType().getTypeName();
                            // Lấy thông tin khách sạn từ phòng đầu tiên có dữ liệu
                            if (hotelName.equals("LuxeHotel") && room.getRoomType().getHotel() != null) {
                                hotelName = room.getRoomType().getHotel().getName();
                                hotelAddress = room.getRoomType().getHotel().getAddress();
                            }
                        }
                        roomInfoList.add(BookingHistoryResponseDTO.RoomInfo.builder()
                                .roomId(room.getId())
                                .roomNumber(room.getRoomNumber())
                                .roomType(rType)
                                .build());
                    }
                    // Giữ trường đơn lẻ từ phòng đầu tiên để tương thích ngược
                    if (!roomInfoList.isEmpty()) {
                        firstRoomId     = roomInfoList.get(0).getRoomId();
                        firstRoomNumber = roomInfoList.get(0).getRoomNumber();
                        firstRoomType   = roomInfoList.get(0).getRoomType();
                    }
                }
            } catch (Exception e) {
                System.err.println("Cảnh báo: Lỗi bóc tách phòng tại Đơn đặt #" + booking.getId());
            }

            long reviewedRoomCount = 0;
            try {
                reviewedRoomCount = reviewRepository.countByBookingId(booking.getId());
            } catch (Exception e) {
                reviewedRoomCount = 0;
            }

            boolean canReview = (booking.getStatus() == BookingStatus.CHECK_OUT)
                    && reviewedRoomCount < Math.max(1, roomInfoList.size());

            return BookingHistoryResponseDTO.builder()
                    .bookingId(booking.getId())
                    .hotelName(hotelName)
                    .hotelAddress(hotelAddress)
                    .roomId(firstRoomId)
                    .roomNumber(firstRoomNumber)
                    .roomType(firstRoomType)
                    .rooms(roomInfoList)
                    .roomCount(roomInfoList.size())
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .totalPrice(booking.getFinalAmount() != null ? booking.getFinalAmount() : java.math.BigDecimal.ZERO)
                    .status(booking.getStatus() != null ? booking.getStatus() : BookingStatus.PENDING)
                    .canReview(canReview)
                    .build();
        }).collect(Collectors.toList());
    }

    private BigDecimal calculateTotalAmount(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        List<RoomPrice> specialPrices = roomPriceRepository.findSpecialPrices(
                room.getRoomType().getId(), checkIn, checkOut);

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            LocalDate currentDate = date;
            BigDecimal dailyPrice = specialPrices.stream()
                    .filter(sp -> !currentDate.isBefore(sp.getStartDate()) && !currentDate.isAfter(sp.getEndDate()))
                    .map(RoomPrice::getSpecialPrice)
                    .findFirst()
                    .orElse(room.getRoomType().getBasePrice());
            total = total.add(dailyPrice);
        }
        return total;
    }

    @Transactional
    public void processRefund(Long bookingId, Long operatorId, String operatorName, String reason) {
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt phòng!"));

        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn!"));

        if (invoice.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hóa đơn chưa được thanh toán, không thể hoàn tiền!");
        }
        if (booking.getStatus() != BookingStatus.CANCELLED && booking.getStatus() != BookingStatus.CHECK_OUT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hoàn tiền được cho đơn đã hủy hoặc đã trả phòng!");
        }

        BigDecimal refundedAmount = invoice.getAmountPaid();
        invoice.setPaymentStatus(PaymentStatus.REFUNDED);
        invoiceRepository.save(invoice);

        // Cập nhật ngay cache daily_revenue_stats để biểu đồ phản ánh đúng sau hoàn tiền
        LocalDate paymentDay = invoice.getPaymentDate() != null
                ? invoice.getPaymentDate().toLocalDate()
                : LocalDate.now();
        BigDecimal updatedRevenue = invoiceRepository.calculateTotalRevenue(
                paymentDay.atStartOfDay(), paymentDay.plusDays(1).atStartOfDay());
        Integer updatedBookings = invoiceRepository.countTotalBookings(
                paymentDay.atStartOfDay(), paymentDay.plusDays(1).atStartOfDay());
        DailyRevenueStat stat = dashboardRepository.findById(paymentDay)
                .orElse(DailyRevenueStat.builder().statDate(paymentDay).build());
        stat.setRevenue(updatedRevenue != null ? updatedRevenue : BigDecimal.ZERO);
        stat.setTotalBookings(updatedBookings != null ? updatedBookings : 0);
        stat.setUpdatedAt(LocalDateTime.now());
        dashboardRepository.save(stat);

        String desc = String.format("Hoàn tiền đơn #%d | Người thao tác: %s | Số tiền: %s | Lý do: %s",
                bookingId, operatorName != null ? operatorName : "N/A", refundedAmount, reason);
        saveAuditLog(operatorId, "REFUND", bookingId, desc);
    }

    private void createInvoice(Booking booking, String method, BigDecimal amount, String bookingSource) {
        Invoice invoice = new Invoice();
        invoice.setBooking(booking);
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(method.toUpperCase());
        } catch (Exception e) {
            paymentMethod = PaymentMethod.CASH;
        }
        invoice.setPaymentMethod(paymentMethod);

        // BANK_TRANSFER và OFFLINE đều được coi là đã thanh toán ngay khi tạo đơn
        boolean isPaidImmediately = "OFFLINE".equalsIgnoreCase(bookingSource)
                || paymentMethod == PaymentMethod.BANK_TRANSFER;

        if (isPaidImmediately) {
            invoice.setPaymentStatus(PaymentStatus.PAID);
            invoice.setAmountPaid(amount);
            invoice.setPaymentDate(LocalDateTime.now());
        } else {
            invoice.setPaymentStatus(PaymentStatus.UNPAID);
            invoice.setAmountPaid(BigDecimal.ZERO);
        }
        invoice.setCreatedAt(LocalDateTime.now());

        invoiceRepository.save(invoice);

        // Đồng bộ dữ liệu RAM phục vụ Controller nhả URL chính xác
        booking.setInvoice(invoice);
    }

    private void saveAuditLog(Long userId, String action, Long targetId, String description) {
        AuditLog auditLog = new AuditLog();

        if (userId != null && userRepository.existsById(userId)) {
            auditLog.setUserId(userId);
        } else {
            auditLog.setUserId(null);
        }

        auditLog.setAction(action);
        auditLog.setTargetId(targetId);
        auditLog.setDescription(description);
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    @Transactional
    public void cancelExpiredBooking(Long bookingId, Long staffId) {
        // 1. Tìm đơn đặt phòng và khóa bản ghi (Pessimistic Lock) chống tranh chấp luồng với Scheduler
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng với ID: " + bookingId));

        // 2. BẪY LỖI: Chỉ cho phép hủy khi đơn chưa Check-in hoặc chưa Check-out
        if (booking.getStatus() == BookingStatus.CHECK_IN || booking.getStatus() == BookingStatus.CHECK_OUT) {
            throw new IllegalStateException("Đơn đặt phòng này đã làm thủ tục nhận phòng hoặc trả phòng, không thể hủy!");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Đơn đặt phòng này đã bị hủy từ trước!");
        }

        // 3. Cập nhật trạng thái đơn đặt sang CANCELLED (Đã hủy)
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // 4. FIX LỖI CÚ PHÁP: Cập nhật trạng thái hóa đơn đi kèm sang CANCELLED
        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn liên kết với đơn đặt phòng này!"));
        invoice.setPaymentStatus(PaymentStatus.CANCELLED); // Đã điền đầy đủ thuộc tính Enum mới
        invoiceRepository.save(invoice);

        // 5. GIẢI PHÓNG PHÒNG: Đưa trạng thái phòng trên ma trận về AVAILABLE ngay lập tức để đón khách mới
        roomRepository.updateRoomStatusByBookingId(bookingId, RoomStatus.AVAILABLE);

        // 6. Ghi Audit Log hệ thống
        saveAuditLog(staffId, "CANCEL_BOOKING", bookingId, "Nhân viên ID " + staffId + " chủ động hủy đơn treo do khách hủy/thoát luồng thanh toán.");
    }

    @Transactional
    public void updatePaymentSuccess(Long bookingId) {
        // 1. Tìm đơn đặt phòng trong DB
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt phòng!"));

        // 2. Logic lật cờ PAID cho Hóa đơn và chuyển đơn sang CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);
        if (booking.getInvoice() != null) {
            booking.getInvoice().setPaymentStatus(PaymentStatus.PAID);
            booking.getInvoice().setPaymentDate(LocalDateTime.now());
            // Bug fix: amountPaid bị bỏ trống → SUM(amountPaid) luôn = 0 trên dashboard
            booking.getInvoice().setAmountPaid(
                booking.getFinalAmount() != null ? booking.getFinalAmount() : BigDecimal.ZERO);
        }
        bookingRepository.save(booking);

        // 3. KÍCH HOẠT AN TOÀN: Chỉ gửi Email SAU KHI DB ĐÃ COMMIT THÀNH CÔNG
        if (booking.getUser() != null && booking.getUser().getEmail() != null) {

            // Tạo một bản sao dữ liệu an toàn để luồng Async không bị dính Lazy Loading Error
            String targetEmail = booking.getUser().getEmail();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Khối lệnh này chỉ chạy khi dữ liệu dưới MySQL đã được bảo toàn 100%
                    emailService.sendBookingSuccessEmail(targetEmail, booking);
                }
            });
        }
    }
    // Trong file BookingService.java của bạn

    public List<Booking> getCustomerBookingHistory(Long userId) {
        // Gọi sang hàm repository có sẵn để lấy danh sách đơn từ mới nhất đến cũ nhất
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<org.example.backend.dto.response.AuditLogDTO> getBookingAuditLogs(Long bookingId) {
        return auditLogRepository.findByTargetIdOrderByCreatedAtDesc(bookingId).stream().map(log -> {
            String operatorName = "Hệ thống";
            if (log.getUserId() != null) {
                operatorName = userRepository.findById(log.getUserId())
                        .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername())
                        .orElse("Người dùng #" + log.getUserId());
            }
            return org.example.backend.dto.response.AuditLogDTO.builder()
                    .action(log.getAction())
                    .actionLabel(mapActionLabel(log.getAction()))
                    .description(log.getDescription())
                    .operatorName(operatorName)
                    .createdAt(log.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private String mapActionLabel(String action) {
        if (action == null) return "Không xác định";
        return switch (action) {
            case "CREATE_BOOKING"    -> "Tạo đơn đặt phòng";
            case "CONFIRM_PAYMENT"   -> "Xác nhận thanh toán";
            case "CANCEL_BOOKING"    -> "Hủy đơn";
            case "CHECK_IN"          -> "Nhận phòng (Check-in)";
            case "CHECK_OUT"         -> "Trả phòng (Check-out)";
            case "REFUND"            -> "Hoàn tiền";
            case "CREATE_BATCH_BOOKING" -> "Tạo đơn nhiều phòng";
            default -> action;
        };
    }

    public List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId) {
        // Gọi xuống tầng Repository để truy vấn DB thực tế
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Booking> getCustomerBookingHistoryByUsername(String username) {
        // 1. Tìm User từ username duy nhất dưới DB
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản khách hàng này!"));

        // 2. Lấy ID thực tế của User đó và gọi hàm Repository có sẵn của bạn
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * Đặt nhiều phòng cùng lúc (batch booking) — tạo 1 Booking với nhiều BookingDetail.
     * Dùng cho tính năng chọn nhiều phòng từ trang yêu thích.
     */
    @Transactional
    public Booking processMultiRoomBooking(BatchBookingRequest request) {
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng!");
        }
        if (request.getRoomIds() == null || request.getRoomIds().isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất 1 phòng!");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;

        // Validate và tính giá tất cả các phòng
        List<Room> rooms = new java.util.ArrayList<>();
        for (Long roomId : request.getRoomIds()) {
            Room room = roomRepository.findByIdWithLock(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Phòng ID " + roomId + " không tồn tại!"));

            if (request.getUserId() != null && roomHoldService.isHeldByOther(roomId, request.getUserId())) {
                throw new IllegalStateException("Phòng " + room.getRoomNumber() + " đang được người khác xử lý. Vui lòng thử lại!");
            }

            boolean occupied = bookingRepository.isRoomOccupied(roomId, request.getCheckIn(), request.getCheckOut());
            if (occupied) {
                throw new IllegalStateException("Phòng " + room.getRoomNumber() + " đã được đặt trong khoảng thời gian này!");
            }

            totalPrice = totalPrice.add(calculateTotalAmount(room, request.getCheckIn(), request.getCheckOut()));
            rooms.add(room);
        }

        // Áp mã giảm giá trên tổng tiền
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = totalPrice;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            try {
                discountAmount = promotionService.calculateDiscount(request.getCouponCode(), totalPrice);
                finalAmount = totalPrice.subtract(discountAmount);
            } catch (Exception e) {
                throw new IllegalArgumentException("Mã giảm giá không hợp lệ hoặc đã hết hạn: " + e.getMessage());
            }
        }

        // Tạo 1 Booking dùng chung cho tất cả phòng
        Booking booking = new Booking();
        if (request.getUserId() != null) {
            booking.setUser(entityManager.getReference(User.class, request.getUserId()));
        }
        booking.setCheckInDate(request.getCheckIn());
        booking.setCheckOutDate(request.getCheckOut());
        booking.setTotalPrice(totalPrice);
        booking.setDiscountAmount(discountAmount);
        booking.setFinalAmount(finalAmount);
        booking.setStatus(BookingStatus.PENDING);

        // Lấy hotel từ phòng đầu tiên
        if (!rooms.isEmpty() && rooms.get(0).getRoomType() != null && rooms.get(0).getRoomType().getHotel() != null) {
            booking.setHotel(rooms.get(0).getRoomType().getHotel());
        }

        Booking savedBooking = bookingRepository.save(booking);

        // Tạo BookingDetail cho từng phòng
        long totalDays = java.time.Duration.between(
                request.getCheckIn().atStartOfDay(), request.getCheckOut().atStartOfDay()).toDays();
        for (Room room : rooms) {
            BigDecimal roomTotal = calculateTotalAmount(room, request.getCheckIn(), request.getCheckOut());
            BigDecimal avgPrice = roomTotal.divide(BigDecimal.valueOf(totalDays > 0 ? totalDays : 1), 2, RoundingMode.HALF_UP);

            BookingDetail detail = new BookingDetail();
            detail.setBooking(savedBooking);
            detail.setRoom(room);
            detail.setPriceAtBooking(avgPrice);
            bookingDetailRepository.save(detail);
        }

        createInvoice(savedBooking, request.getPaymentMethod(), finalAmount, "ONLINE");

        // Gửi email hóa đơn ngay nếu BANK_TRANSFER (invoice đã PAID tức thì)
        if ("BANK_TRANSFER".equalsIgnoreCase(request.getPaymentMethod())) {
            String toEmail = null;
            try { toEmail = savedBooking.getUser() != null ? savedBooking.getUser().getEmail() : null; } catch (Exception ignored) {}
            if (toEmail != null && !toEmail.isBlank()) {
                Room firstRoom = rooms.get(0);
                String rNum = firstRoom.getRoomNumber();
                String rType = firstRoom.getRoomType() != null ? firstRoom.getRoomType().getTypeName() : "N/A";
                String hName = (firstRoom.getRoomType() != null && firstRoom.getRoomType().getHotel() != null)
                        ? firstRoom.getRoomType().getHotel().getName() : "LuxeHotel";
                final String fe = toEmail, fr = rNum, ft = rType, fh = hName;
                final Invoice finalInv = savedBooking.getInvoice();
                final Booking fb = savedBooking;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() {
                        emailService.sendInvoiceEmail(fe, fb, finalInv, fr, ft, fh);
                    }
                });
            }
        }

        saveAuditLog(request.getUserId(), "CREATE_BATCH_BOOKING", savedBooking.getId(),
                String.format("Đặt %d phòng cùng lúc. Tổng gốc: %s, Thực trả: %s", rooms.size(), totalPrice, finalAmount));

        return savedBooking;
    }
}