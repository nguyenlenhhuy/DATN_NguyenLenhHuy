package org.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRoomResponseDTO {
    private Long roomId;
    private String roomNumber;
    private String typeName;
    private Double price;
    private String description;
    private String mainImageUrl;
    private List<String> albumImages; // Chứa danh sách chuỗi URL/Base64 hình ảnh chi tiết
}