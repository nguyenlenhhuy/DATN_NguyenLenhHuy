package org.example.backend.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@AllArgsConstructor
@Data
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String action;

    @Column(name = "target_id")
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructor nhanh để tiện tạo object
    public AuditLog() {}
    public AuditLog(Long userId, String action, Long targetId, String description) {
        this.userId = userId;
        this.action = action;
        this.targetId = targetId;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }


}