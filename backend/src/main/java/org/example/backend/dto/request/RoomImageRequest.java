package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoomImageRequest {
    @NotBlank(message = "Đường dẫn cấu hình ảnh không được để trống")
    private String imageUrl;
    private Boolean isPrimary = false;
}