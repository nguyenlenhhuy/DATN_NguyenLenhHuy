package org.example.backend.dto.request;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromotionRequestDTO {
    private String code;
    private Integer discountPercentage;
    private LocalDate startDate;
    private LocalDate endDate;
}