package org.example.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter @Setter
@AllArgsConstructor
public class RoomTypeRequest {
    @NotNull(message = "Hotel ID không được để trống")
    private Long hotelId;

    @NotBlank(message = "Tên loại phòng không được để trống")
    @Size(max = 100)
    private String typeName;

    @NotNull(message = "Giá cơ bản không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phòng phải lớn hơn 0")
    private BigDecimal basePrice;

    @NotNull(message = "Sức chứa tối đa không được để trống")
    @Min(value = 1, message = "Sức chứa phải tối thiểu là 1 người")
    private Integer maxOccupancy;

    private Boolean isFeatured = false;

}