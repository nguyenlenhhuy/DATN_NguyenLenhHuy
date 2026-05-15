package org.example.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RoomRequest {
    @NotNull(message = "Hotel ID không được để trống")
    private Long hotelId;

    @NotNull(message = "Room Type ID không được để trống")
    private Long roomTypeId;

    @NotBlank(message = "Số phòng không được để trống")
    @Size(max = 10)
    private String roomNumber;

    private Integer floor;
}
