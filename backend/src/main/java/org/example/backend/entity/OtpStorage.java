package org.example.backend.entity;


import org.example.backend.entity.enums.OtpType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_storage") @Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpStorage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    @Column(name = "otp_code", length = 6) private String otpCode;
    @Column(name = "expiry_time") private LocalDateTime expiryTime;
    @Enumerated(EnumType.STRING)
    private OtpType otpType;
}
