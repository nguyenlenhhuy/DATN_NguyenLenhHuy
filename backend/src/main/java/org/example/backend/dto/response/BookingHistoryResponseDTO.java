package org.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.entity.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingHistoryResponseDTO {
    private Long bookingId;
    private String hotelName;
    private String hotelAddress;

    // Phòng đầu tiên — dùng cho review modal và re-book single room
    private Long roomId;
    private String roomNumber;
    private String roomType;

    // Danh sách đầy đủ tất cả phòng trong đơn (hỗ trợ batch booking)
    private List<RoomInfo> rooms;
    private int roomCount;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private boolean canReview;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomInfo {
        private Long roomId;
        private String roomNumber;
        private String roomType;
    }
}