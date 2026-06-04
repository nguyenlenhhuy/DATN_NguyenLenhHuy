package org.example.backend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.entity.enums.BookingStatus;
import org.example.backend.entity.enums.PaymentStatus;
import org.example.backend.entity.enums.RoomStatus;
import org.example.backend.repository.BookingRepository;
import org.example.backend.repository.InvoiceRepository;
import org.example.backend.repository.RoomRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupScheduler {

    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;

    /**
     * Tự động chạy mỗi 30 giây một lần (fixedRate = 30000 ms)
     * Tìm và hủy các đơn hàng PENDING đã ngâm quá 5 phút
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void autoCancelExpiredBookings() {
        // Mốc thời gian quá hạn = Thời gian hiện tại lùi lại 5 phút
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        // Bước 1: Tìm ra danh sách ID đơn hàng bị quá hạn dựa trên cờ PENDING
        List<Long> expiredBookingIds = bookingRepository.findExpiredBookingIds(BookingStatus.PENDING, threshold);

        if (expiredBookingIds != null && !expiredBookingIds.isEmpty()) {

            // Bước 2: Giải phóng trạng thái phòng vật lý trên sơ đồ ma trận về AVAILABLE
            roomRepository.releaseRoomsByBookingIds(RoomStatus.AVAILABLE, expiredBookingIds);

            // Bước 3: Hủy toàn bộ hóa đơn liên kết của các đơn hàng này sang CANCELLED
            invoiceRepository.cancelInvoicesByBookingIds(PaymentStatus.CANCELLED, expiredBookingIds);

            // Bước 4: Thực thi cập nhật hàng loạt trạng thái đơn sang CANCELLED dưới DB
            int affectedRows = bookingRepository.cancelExpiredBookings(threshold);

            if (affectedRows > 0) {
                log.info("⏰ [SCHEDULER] Hệ thống tự động: Đã quét dọn và hủy {} đơn đặt phòng treo quá hạn 5 phút thành công.", affectedRows);
            }
        }
    }
}