package org.example.backend.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {
    private String action;
    private String actionLabel;
    private String description;
    private String operatorName;
    private LocalDateTime createdAt;
}
