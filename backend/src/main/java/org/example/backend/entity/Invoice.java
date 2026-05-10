package org.example.backend.entity;

import org.example.backend.entity.enums.PaymentMethod;
import org.example.backend.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id", unique = true)
    private Booking booking;

    @Enumerated(EnumType.STRING) // Giúp lưu "VNPAY", "CASH" vào DB thay vì 0, 1
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "amount_paid", precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING) // Giúp lưu "PAID", "UNPAID" vào DB
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    // THÊM TRƯỜNG NÀY ĐỂ HẾT LỖI GẠCH ĐỎ TRONG SERVICE
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Tự động gán ngày tạo khi lưu vào DB (Cực kỳ tiện lợi)
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}