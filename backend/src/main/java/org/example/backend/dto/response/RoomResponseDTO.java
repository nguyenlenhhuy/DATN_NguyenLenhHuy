package org.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDTO {
    private Long roomId;
    private String roomNumber;
    private Integer floor;
    private String status;
    private String typeName;
    private BigDecimal price;
    private String hotelName;
    private String imageUrl;         // Ảnh đại diện hiển thị ngoài Card phòng
    private List<String> albumImages; // Danh sách toàn bộ ảnh phục vụ Modal chỉnh sửa
    private boolean isDeleted;
}