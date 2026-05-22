package org.example.backend.dto.response;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromotionResponseDTO {
    private Long id;
    private String code;
    private Integer discountPercentage;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
}