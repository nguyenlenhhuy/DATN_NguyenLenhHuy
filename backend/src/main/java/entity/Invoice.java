package entity;

import entity.enums.PaymentMethod;
import entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "invoices") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne @JoinColumn(name = "booking_id", unique = true) private Booking booking;
    private PaymentMethod paymentMethod;
    @Column(name = "amount_paid", precision = 15, scale = 2) private BigDecimal amountPaid;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDate;
}