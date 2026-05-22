package org.example.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class RoomBatchRequest {
    @NotNull(message = "Mã khách sạn không được để trống")
    private Long hotelId;

    @NotNull(message = "Mã loại phòng không được để trống")
    private Long roomTypeId;

    @NotNull(message = "Tầng không được để trống")
    @Min(value = 0, message = "Số tầng tối thiểu là tầng 0")
    private Integer floor;

    @NotNull(message = "Số phòng bắt đầu không được để trống")
    private Integer startRoomNumber;

    @NotNull(message = "Số lượng phòng muốn tạo không được để trống")
    @Min(value = 1, message = "Phải tạo ít nhất 1 phòng")
    private Integer totalRooms;

    @Valid
    private List<RoomImageRequest> images; // Bộ sưu tập ảnh dùng chung cho cả loạt phòng này
}