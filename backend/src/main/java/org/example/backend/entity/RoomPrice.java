package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "room_prices") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RoomPrice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "room_type_id") private RoomType roomType;
    @Column(name = "special_price", precision = 15, scale = 2) private BigDecimal specialPrice;
    private LocalDate startDate;
    private LocalDate endDate;
}
