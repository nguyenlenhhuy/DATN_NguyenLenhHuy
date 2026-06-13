package org.example.backend.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.BookingRequest;
import org.example.backend.dto.response.BookingHistoryResponseDTO;
import org.example.backend.entity.*;
import org.example.backend.entity.enums.BookingStatus;
import org.example.backend.entity.enums.PaymentMethod;
import org.example.backend.entity.enums.PaymentStatus;
import org.example.backend.entity.enums.RoomStatus;
import org.example.backend.exception.ResourceNotFoundException;
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

    // 🔥 TIÊM THÊM: PromotionService để xử lý logic re-validate mã giảm giá
    private final PromotionService promotionService;
    private final EmailService emailService;

    /**
     * 1. CHỨC NĂNG ĐẶT PHÒNG TRỰC TUYẾN
     */
    @Transactional
    public Booking processBooking(BookingRequest request, String bookingSource) {
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng!");
        }

        // Sử dụng Pessimistic Lock để chặn đứng lỗi tranh chấp phòng (Race Condition)
        Room room = roomRepository.findByIdWithLock(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại!"));

        // Kiểm tra trùng lịch đặt phòng (Chống Overbooking)
        boolean occupied = bookingRepository.isRoomOccupied(
                room.getId(), request.getCheckIn(), request.getCheckOut());

        if (occupied) {
            throw new IllegalStateException("Phòng đã bị đặt hoặc đang bảo trì trong khoảng thời gian này!");
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

        // Ghi lại vết hệ thống
        saveAuditLog(request.getUserId(), "CREATE_BOOKING", savedBooking.getId(),
                String.format("Tạo đơn đặt phòng thành công qua %s. Tổng gốc: %s, Thực trả: %s", bookingSource, totalPrice, finalAmount));

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
            // 🌟 SỬA ĐỒNG BỘ: Khi checkout thanh toán tại quầy, lấy đúng số tiền đã trừ voucher
            invoice.setAmountPaid(booking.getFinalAmount());
            invoice.setPaymentDate(LocalDateTime.now());
            invoiceRepository.save(invoice);
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
            // 1. Thiết lập giá trị phòng ngừa mặc định ban đầu nếu dữ liệu DB bị khuyết thiếu
            String hotelName = "LuxeHotel";
            String hotelAddress = "Hệ thống trực tuyến";
            String roomNumber = "N/A"; // Giá trị fallback cho Số phòng
            String roomType = "N/A";   // Giá trị fallback cho Loại phòng
            Long roomId = null;        // Biến tạm hứng ID phòng vật lý phục vụ Angular Router

            try {
                // 2. Kiểm tra và bóc tách an toàn chuỗi liên kết để tránh triệt để lỗi NullPointerException
                if (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty()) {
                    BookingDetail firstDetail = booking.getBookingDetails().stream().findFirst().orElse(null);

                    if (firstDetail != null && firstDetail.getRoom() != null) {
                        // BÓC TÁCH ID VÀ SỐ PHÒNG TRỰC TIẾP TỪ THỰC THỂ ROOM
                        roomId = firstDetail.getRoom().getId();
                        roomNumber = firstDetail.getRoom().getRoomNumber();

                        if (firstDetail.getRoom().getRoomType() != null) {
                            // Trích xuất tên hạng phòng (loại phòng)
                            roomType = firstDetail.getRoom().getRoomType().getTypeName();

                            // Trích xuất thông tin khách sạn chi nhánh tương ứng
                            if (firstDetail.getRoom().getRoomType().getHotel() != null) {
                                hotelName = firstDetail.getRoom().getRoomType().getHotel().getName();
                                hotelAddress = firstDetail.getRoom().getRoomType().getHotel().getAddress();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ghi nhận vết lỗi hệ thống nhưng giữ luồng chạy, không làm sập toàn bộ API lịch sử của người dùng
                System.err.println("Cảnh báo: Lỗi bóc tách liên kết dữ liệu Khách sạn & Phòng tại Đơn đặt #" + booking.getId());
            }

            // 3. Kiểm tra trạng thái đánh giá dịch vụ dựa trên mã đơn hàng
            boolean hasReviewed = false;
            try {
                hasReviewed = reviewRepository.existsByBookingId(booking.getId());
            } catch (Exception e) {
                hasReviewed = false;
            }

            // Điều kiện viết đánh giá: Đã thực hiện Check-out xong và chưa từng viết review trước đó
            // Đảm bảo trạng thái so khớp lý tưởng với BookingStatus.CHECK_OUT (hoặc CHECKED_OUT tùy thiết kế Enum của bạn)
            boolean canReview = (booking.getStatus() == BookingStatus.CHECK_OUT) && !hasReviewed;

            // 4. Trả về đối tượng Builder DTO phẳng khớp hoàn hảo với phân hệ giao diện Timeline
            return BookingHistoryResponseDTO.builder()
                    .bookingId(booking.getId())
                    .hotelName(hotelName)
                    .hotelAddress(hotelAddress)
                    .roomId(roomId)         // ĐÃ ĐỒNG BỘ ID PHÒNG SANG BUILDER
                    .roomNumber(roomNumber) // Đã đồng bộ trường số phòng lên DTO
                    .roomType(roomType)     // Đã đồng bộ trường loại phòng lên DTO
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

    private void createInvoice(Booking booking, String method, BigDecimal amount, String bookingSource) {
        Invoice invoice = new Invoice();
        invoice.setBooking(booking);
        try {
            invoice.setPaymentMethod(PaymentMethod.valueOf(method.toUpperCase()));
        } catch (Exception e) {
            invoice.setPaymentMethod(PaymentMethod.CASH);
        }

        if ("OFFLINE".equalsIgnoreCase(bookingSource)) {
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
}