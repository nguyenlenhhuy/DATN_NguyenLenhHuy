package org.example.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeImageRequest {
    private String imageUrl;
    private Boolean isPrimary = false; // Mặc định là false nếu không truyền
}