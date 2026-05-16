package org.example.backend.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.BookingRequest;
import org.example.backend.dto.response.BookingHistoryResponseDTO;
import org.example.backend.entity.*;
import org.example.backend.entity.enums.BookingStatus;
import org.example.backend.entity.enums.PaymentMethod;
import org.example.backend.entity.enums.PaymentStatus;
import org.example.backend.entity.enums.RoomStatus; // Đã import Enum của bạn
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    /**
     * 1. CHỨC NĂNG ĐẶT PHÒNG
     */
    @Transactional
    public Booking processBooking(BookingRequest request, String bookingSource) {
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng!");
        }

        Room room = roomRepository.findByIdWithLock(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại!"));

        boolean occupied = bookingRepository.isRoomOccupied(
                room.getId(), request.getCheckIn(), request.getCheckOut());

        if (occupied) {
            throw new IllegalStateException("Phòng đã bị đặt hoặc đang bảo trì trong khoảng thời gian này!");
        }

        BigDecimal totalAmount = calculateTotalAmount(room, request.getCheckIn(), request.getCheckOut());

        Booking booking = new Booking();
        booking.setUser(entityManager.getReference(User.class, request.getUserId()));
        booking.setCheckInDate(request.getCheckIn());
        booking.setCheckOutDate(request.getCheckOut());
        booking.setTotalPrice(totalAmount);

        if ("OFFLINE".equalsIgnoreCase(bookingSource)) {
            booking.setStatus(BookingStatus.CONFIRMED);
        } else {
            booking.setStatus(BookingStatus.PENDING);
        }

        Booking savedBooking = bookingRepository.save(booking);

        long totalDays = Duration.between(request.getCheckIn().atStartOfDay(), request.getCheckOut().atStartOfDay()).toDays();
        BigDecimal avgPriceSnapshot = totalAmount.divide(BigDecimal.valueOf(totalDays > 0 ? totalDays : 1), 2, BigDecimal.ROUND_HALF_UP);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(savedBooking);
        detail.setRoom(room);
        detail.setPriceAtBooking(avgPriceSnapshot);
        bookingDetailRepository.save(detail);

        createInvoice(savedBooking, request.getPaymentMethod(), totalAmount, bookingSource);

        saveAuditLog(request.getUserId(), "CREATE_BOOKING", savedBooking.getId(),
                String.format("Tạo đơn đặt phòng thành công qua %s. Tổng: %s", bookingSource, totalAmount));

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
        invoice.setAmountPaid(booking.getTotalPrice());
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

        // Cập nhật trạng thái phòng bằng Enum RoomStatus
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
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Đơn phải ở trạng thái CONFIRMED mới được Check-in!");
        }

        booking.setStatus(BookingStatus.CHECK_IN);
        bookingRepository.save(booking);

        // Chuyển trạng thái phòng sang OCCUPIED (Đang sử dụng)
        roomRepository.updateRoomStatusByBookingId(bookingId, RoomStatus.OCCUPIED);

        saveAuditLog(staffId, "CHECK_IN", bookingId, "Check-in thành công.");
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

        // Chuyển trạng thái phòng sang DIRTY (Cần dọn dẹp)
        roomRepository.updateRoomStatusByBookingId(bookingId, RoomStatus.DIRTY);

        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));

        if (invoice.getPaymentStatus() != PaymentStatus.PAID) {
            invoice.setPaymentStatus(PaymentStatus.PAID);
            invoice.setAmountPaid(booking.getTotalPrice());
            invoice.setPaymentDate(LocalDateTime.now());
            invoiceRepository.save(invoice);
        }

        saveAuditLog(staffId, "CHECK_OUT", bookingId, "Check-out thành công.");
    }

    /**
     * 6. LỊCH SỬ ĐẶT PHÒNG
     */
    public List<BookingHistoryResponseDTO> getBookingHistory(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return bookings.stream().map(booking -> {
            BookingDetail firstDetail = booking.getBookingDetails().stream().findFirst().orElse(null);
            String hotelName = "N/A";
            String hotelAddress = "N/A";

            if (firstDetail != null && firstDetail.getRoom() != null) {
                hotelName = firstDetail.getRoom().getRoomType().getHotel().getName();
                hotelAddress = firstDetail.getRoom().getRoomType().getHotel().getAddress();
            }

            boolean hasReviewed = reviewRepository.existsByBookingId(booking.getId());
            boolean canReview = (booking.getStatus() == BookingStatus.CHECK_OUT) && !hasReviewed;

            return BookingHistoryResponseDTO.builder()
                    .bookingId(booking.getId())
                    .hotelName(hotelName)
                    .hotelAddress(hotelAddress)
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .totalPrice(booking.getTotalPrice())
                    .status(booking.getStatus())
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
    }

    private void saveAuditLog(Long userId, String action, Long targetId, String description) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setTargetId(targetId);
        auditLog.setDescription(description);
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }
}