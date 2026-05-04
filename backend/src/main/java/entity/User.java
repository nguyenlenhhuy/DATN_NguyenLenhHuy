package entity;

import entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.LocalDateTime;

    @Entity
    @Table(name = "users")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public class User {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        @Column(unique = true, nullable = false) private String username;
        @Column(unique = true, nullable = false) private String email;
        @Column(name = "password_hash", nullable = false) private String password;
        @Column(name = "full_name", nullable = false) private String fullName;
        private String phone;
        @ManyToOne @JoinColumn(name = "role_id", nullable = false) private Role role;
        @Enumerated(EnumType.STRING) private UserStatus status = UserStatus.PENDING;
        @Column(name = "is_deleted") private Boolean isDeleted = false;
        @Column(name = "created_at", updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    }
