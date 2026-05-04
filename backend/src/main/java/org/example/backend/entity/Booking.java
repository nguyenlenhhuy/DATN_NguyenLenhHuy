package org.example.backend.entity;

import org.example.backend.entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "bookings") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "user_id") private User user;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    @Column(name = "total_price", precision = 15, scale = 2) private BigDecimal totalPrice;
    private BookingStatus status = BookingStatus.PENDING;
}
