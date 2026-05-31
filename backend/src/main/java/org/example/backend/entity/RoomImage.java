package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference; // ĐÃ THÊM IMPORT NÀY
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "room_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CHỈNH SỬA TẠI ĐÂY: Thêm @JsonBackReference để chặn Jackson không tuần tự hóa ngược lại Room, giải quyết triệt để StackOverflowError
    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    @JsonBackReference
    private Room room;

    @Column(name = "image_url", columnDefinition = "LONGTEXT", nullable = false)
    private String imageUrl;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;
}