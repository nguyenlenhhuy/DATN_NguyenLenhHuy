package org.example.backend.entity;

import org.example.backend.entity.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hotel_id", "room_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    private Integer floor;

    @Enumerated(EnumType.STRING) // Giúp lưu "AVAILABLE" vào DB thay vì số 0
    @Column(name = "status", nullable = false)
    private RoomStatus status = RoomStatus.AVAILABLE;

    // Helper method để lấy Hotel ID nhanh mà không bị lỗi "Cannot resolve getHotelId"
    public Long getHotelId() {
        return this.hotel != null ? this.hotel.getId() : null;
    }

    // Helper method để lấy RoomType ID nhanh
    public Long getRoomTypeId() {
        return this.roomType != null ? this.roomType.getId() : null;
    }
}