package org.example.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class RoomRequest {
    @NotNull(message = "Mã khách sạn không được để trống")
    private Long hotelId;

    @NotNull(message = "Mã loại phòng không được để trống")
    private Long roomTypeId;

    @NotBlank(message = "Số phòng không được để trống")
    @Size(max = 10, message = "Số phòng không vượt quá 10 ký tự")
    private String roomNumber;

    @NotNull(message = "Số tầng không được để trống")
    @Min(value = 0, message = "Số tầng tối thiểu là tầng 0")
    private Integer floor;

    @Valid
    private List<RoomImageRequest> images;
}