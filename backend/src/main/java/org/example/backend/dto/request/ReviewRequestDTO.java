package org.example.backend.dto.request;

import jakarta.validation.constraints.*; // Sử dụng jakarta cho Spring Boot 3+
import lombok.Data;
import java.util.List;

@Data
public class ReviewRequestDTO {

    @NotNull(message = "Mã đặt phòng không được trống")
    private Long bookingId;

    @Min(value = 1, message = "Đánh giá thấp nhất là 1 sao")
    @Max(value = 5, message = "Đánh giá cao nhất là 5 sao")
    @NotNull(message = "Vui lòng chọn số sao")
    private Integer rating;

    @NotBlank(message = "Nội dung nhận xét không được để trống")
    @Size(max = 1000, message = "Nội dung không được quá 1000 ký tự")
    private String comment;

    private List<String> mediaUrls;
}