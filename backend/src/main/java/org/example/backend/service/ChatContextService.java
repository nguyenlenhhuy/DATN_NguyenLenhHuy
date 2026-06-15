package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.entity.Booking;
import org.example.backend.entity.Room;
import org.example.backend.entity.enums.RoomStatus;
import org.example.backend.repository.BookingRepository;
import org.example.backend.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Xây dựng ngữ cảnh động (RAG) từ dữ liệu MySQL thực để cung cấp cho AI.
 * Dữ liệu được lọc theo vai trò người dùng để bảo mật thông tin nhạy cảm.
 */
@Service
@RequiredArgsConstructor
public class ChatContextService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public String buildContext(String userRole, Long userId) {
        StringBuilder ctx = new StringBuilder();
        String role = userRole != null ? userRole.toUpperCase() : "GUEST";

        // --- Phòng trống (dữ liệu luôn có, giới hạn 15 phòng để giữ context nhỏ) ---
        List<Room> available = roomRepository.findAllByStatus(RoomStatus.AVAILABLE);
        ctx.append("=== PHÒNG ĐANG TRỐNG (").append(available.size()).append(" phòng) ===\n");
        available.stream().limit(15).forEach(r -> {
            String typeName = (r.getRoomType() != null && r.getRoomType().getTypeName() != null)
                    ? r.getRoomType().getTypeName() : "Tiêu chuẩn";

            // Ưu tiên giá của phòng, fallback sang giá gốc của loại phòng
            double price = 0;
            if (r.getPrice() != null && r.getPrice() > 0) {
                price = r.getPrice();
            } else if (r.getRoomType() != null && r.getRoomType().getBasePrice() != null) {
                price = r.getRoomType().getBasePrice().doubleValue();
            }

            Integer maxOccupancy = (r.getRoomType() != null) ? r.getRoomType().getMaxOccupancy() : null;
            String hotelName = (r.getHotel() != null && r.getHotel().getName() != null)
                    ? r.getHotel().getName() : "LuxeHotel";
            Integer starRating = (r.getHotel() != null) ? r.getHotel().getStarRating() : null;

            ctx.append(String.format(
                    "- Phòng %s | Loại: %s | Tầng: %s | Sức chứa: %s người | Giá: %,.0f VND/đêm | Khách sạn: %s%s | ID: %d%n",
                    r.getRoomNumber(),
                    typeName,
                    r.getFloor() != null ? r.getFloor() : "?",
                    maxOccupancy != null ? maxOccupancy : "?",
                    price,
                    hotelName,
                    starRating != null ? " (" + starRating + " sao)" : "",
                    r.getId()));
        });

        // --- Dữ liệu vận hành (chỉ cho STAFF và ADMIN) ---
        if ("STAFF".equals(role) || "ADMIN".equals(role)) {
            long dirty       = roomRepository.countByStatusAndNotDeleted(RoomStatus.DIRTY);
            long occupied    = roomRepository.countByStatusAndNotDeleted(RoomStatus.OCCUPIED);
            long maintenance = roomRepository.countByStatusAndNotDeleted(RoomStatus.MAINTENANCE);
            long reserved    = roomRepository.countByStatusAndNotDeleted(RoomStatus.RESERVED);

            ctx.append("\n=== TÌNH TRẠNG VẬN HÀNH ===\n");
            ctx.append("Phòng đang có khách (OCCUPIED):   ").append(occupied).append("\n");
            ctx.append("Phòng cần dọn dẹp  (DIRTY):       ").append(dirty).append("\n");
            ctx.append("Phòng đang bảo trì (MAINTENANCE): ").append(maintenance).append("\n");
            ctx.append("Phòng đã đặt trước (RESERVED):    ").append(reserved).append("\n");
            ctx.append("Phòng trống        (AVAILABLE):   ").append(available.size()).append("\n");
        }

        // --- Đơn đặt phòng cá nhân (chỉ cho CUSTOMER đã đăng nhập) ---
        if ("CUSTOMER".equals(role) && userId != null) {
            List<Booking> myBookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (!myBookings.isEmpty()) {
                ctx.append("\n=== ĐƠN ĐẶT PHÒNG CỦA KHÁCH ===\n");
                myBookings.stream().limit(3).forEach(b ->
                    ctx.append(String.format("- Đơn #%d | Check-in: %s | Check-out: %s | Trạng thái: %s%n",
                            b.getId(),
                            b.getCheckInDate(),
                            b.getCheckOutDate(),
                            b.getStatus() != null ? b.getStatus().name() : "UNKNOWN"))
                );
            }
        }

        return ctx.toString();
    }
}
