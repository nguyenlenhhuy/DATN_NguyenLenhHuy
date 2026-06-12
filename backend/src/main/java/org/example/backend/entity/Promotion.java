package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_percentage", nullable = false)
    private Integer discountPercentage;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @ManyToMany(mappedBy = "promotions", fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore // Chống vòng lặp vô hạn khi parse JSON
    private java.util.Set<Room> rooms = new java.util.HashSet<>();
}