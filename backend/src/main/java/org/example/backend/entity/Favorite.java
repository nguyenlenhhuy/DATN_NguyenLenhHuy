package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // ĐÃ THÊM IMPORT NÀY
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "room_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CHỈNH SỬA TẠI ĐÂY: Thêm @JsonIgnoreProperties cho cả User và Room
    // vì cả hai đều đang dùng FetchType.LAZY (dễ dính lỗi Proxy khi parse JSON)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Room room;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}