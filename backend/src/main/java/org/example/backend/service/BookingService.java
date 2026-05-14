package org.example.backend.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.BookingRequest;
import org.example.backend.dto.response.BookingHistoryResponseDTO;
import org.example.backend.entity.*;
import org.example.backend.entity.enums.BookingStatus;
import org.example.backend.entity.enums.PaymentMethod;
import org.example.backend.entity.enums.PaymentStatus;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final EntityManager entityManager;

    /**
     * Chức năng Đặt phòng Online (Hoàn thiện)
     */
    @Transactional
    public Booking processBooking(BookingRequest request) {

        // 1. Khóa bản ghi Room (Pessimistic Lock) để chống đặt trùng cùng lúc
        Room room = roomRepository.findByIdWithLock(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại!"));

        // 2. Kiểm tra ngày đặt
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new RuntimeException("Ngày trả phòng phải sau ngày nhận phòng!");
        }

        // 3. Double Check phòng trống trong DB
        boolean occupied = bookingRepository.isRoomOccupied(
                room.getId(), request.getCheckIn(), request.getCheckOut());

        if (occupied) {
            throw new RuntimeException("Phòng đã bị đặt bởi người khác trong khoảng thời gian này!");
        }

        // 4. Tính toán giá linh hoạt (Giá gốc + Giá đặc biệt theo ngày)
        BigDecimal totalAmount = calculateTotalAmount(room, request.getCheckIn(), request.getCheckOut());

        // 5. Khởi tạo Booking
        Booking booking = new Booking();
        // Dùng getReference để không cần select User từ DB (chỉ lấy proxy ID)
        booking.setUser(entityManager.getReference(User.class, request.getUserId()));
        booking.setCheckInDate(request.getCheckIn());
        booking.setCheckOutDate(request.getCheckOut());
        booking.setTotalPrice(totalAmount);
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        // 6. Lưu BookingDetail (Snapshot giá tại thời điểm đặt để đối soát sau này)
        BookingDetail detail = new BookingDetail();
        detail.setBooking(savedBooking);
        detail.setRoom(room);
        detail.setPriceAtBooking(room.getRoomType().getBasePrice());
        bookingDetailRepository.save(detail);

        // 7. Tạo hóa đơn (Invoice)
        createInvoice(savedBooking, request.getPaymentMethod(), totalAmount);

        return savedBooking;
    }

    /**
     * Logic tính tiền chi tiết từng ngày
     */
    private BigDecimal calculateTotalAmount(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        List<RoomPrice> specialPrices = roomPriceRepository.findSpecialPrices(
                room.getRoomType().getId(), checkIn, checkOut);

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            LocalDate currentDate = date;

            // Tìm xem ngày hiện tại có giá đặc biệt nào áp dụng không
            BigDecimal dailyPrice = specialPrices.stream()
                    .filter(sp -> !currentDate.isBefore(sp.getStartDate()) && !currentDate.isAfter(sp.getEndDate()))
                    .map(RoomPrice::getSpecialPrice)
                    .findFirst()
                    .orElse(room.getRoomType().getBasePrice()); // Nếu không có thì lấy giá gốc

            total = total.add(dailyPrice);
        }
        return total;
    }

    private void createInvoice(Booking booking, String method, BigDecimal amount) {
        Invoice invoice = new Invoice();
        invoice.setBooking(booking);
        try {
            invoice.setPaymentMethod(PaymentMethod.valueOf(method.toUpperCase()));
        } catch (Exception e) {
            invoice.setPaymentMethod(PaymentMethod.CASH);
        }
        invoice.setAmountPaid(BigDecimal.ZERO);
        invoice.setPaymentStatus(PaymentStatus.UNPAID);
        invoice.setCreatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }

    /**
     * Chức năng xác nhận đã thanh toán (Dùng cho Webhook VNPAY)
     */
    @Transactional
    public void confirmPayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        booking.setStatus(BookingStatus.CONFIRMED);

        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        invoice.setPaymentStatus(PaymentStatus.PAID);
        invoice.setAmountPaid(booking.getTotalPrice());
        invoice.setPaymentDate(LocalDateTime.now());

        bookingRepository.save(booking);
        invoiceRepository.save(invoice);
    }

    /**
     * Chức năng Hủy đặt phòng
     */
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        if (booking.getStatus() == BookingStatus.CHECK_IN || booking.getStatus() == BookingStatus.CHECK_OUT) {
            throw new RuntimeException("Không thể hủy phòng đã nhận hoặc đã trả!");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Cập nhật trạng thái hóa đơn nếu có
        invoiceRepository.findByBookingId(bookingId).ifPresent(inv -> {
            inv.setPaymentStatus(PaymentStatus.REFUNDED); // Giả định hoàn tiền hoặc hủy thu
            invoiceRepository.save(inv);
        });
    }

    /**
     * Lấy lịch sử đặt phòng
     */
    public List<BookingHistoryResponseDTO> getBookingHistory(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return bookings.stream().map(booking -> {
            // Lấy thông tin chi tiết đầu tiên (thường 1 booking có 1 phòng trong hệ thống của bạn)
            BookingDetail firstDetail = booking.getBookingDetails().stream()
                    .findFirst()
                    .orElse(null);

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
                    .hotelName(hotelName)       // Đã sửa
                    .hotelAddress(hotelAddress) // Đã sửa
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .totalPrice(booking.getTotalPrice())
                    .status(booking.getStatus())
                    .canReview(canReview)
                    .build();
        }).collect(Collectors.toList());
    }
}