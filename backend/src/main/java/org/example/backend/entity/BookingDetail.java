package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.math.BigDecimal;

@Entity
@Table(name = "booking_details") @Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "booking_id") private Booking booking;
    @ManyToOne @JoinColumn(name = "room_id") private Room room;
    @Column(name = "price_at_booking", precision = 15, scale = 2) private BigDecimal priceAtBooking;
}