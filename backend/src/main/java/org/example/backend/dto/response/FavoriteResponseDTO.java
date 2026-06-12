package org.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponseDTO {
    private Long id;
    private RoomFavDTO room; // Sử dụng cấu hình DTO phòng tùy biến để mang theo Khuyến mãi

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomFavDTO {
        private Long roomId;
        private String roomNumber;
        private Integer floor;
        private Double price;
        private String status;
        private String typeName;
        private String imageUrl;
        private PromotionResponseDTO appliedPromotion; // Gắn thông tin khuyến mãi sạch vào đây
        private Integer maxGuests;
        private Double rating;
        private Integer reviewCount;
    }
}