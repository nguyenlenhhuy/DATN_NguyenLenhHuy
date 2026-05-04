package org.example.backend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity @Table(name = "room_types") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RoomType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "hotel_id") private Hotel hotel;
    @Column(name = "type_name") private String typeName;
    @Column(name = "base_price", precision = 15, scale = 2) private BigDecimal basePrice;
    @Column(name = "max_occupancy") private Integer maxOccupancy;
}
