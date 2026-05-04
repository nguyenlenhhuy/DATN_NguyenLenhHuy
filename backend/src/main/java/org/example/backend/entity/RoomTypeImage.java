package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity @Table(name = "room_type_images") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RoomTypeImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "room_type_id") private RoomType roomType;
    @Column(name = "image_url") private String imageUrl;
    @Column(name = "is_primary") private Boolean isPrimary = false;
}
