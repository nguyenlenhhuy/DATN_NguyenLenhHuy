package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.backend.entity.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

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
    @JsonIgnore
    private Hotel hotel;

    @ManyToOne
    @JoinColumn(name = "room_type_id", nullable = false)
    @JsonIgnore
    private RoomType roomType;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    private Integer floor;

    // ĐÃ BỔ SUNG: Trường giá riêng biệt cho từng phòng (Giải quyết lỗi 'Cannot resolve method getPrice')
    @Column(name = "price")
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status = RoomStatus.AVAILABLE;

    // ĐÃ BỔ SUNG: Mối quan hệ liên kết lấy danh sách bộ sưu tập ảnh của phòng (Giải quyết lỗi 'Cannot resolve method getImages')
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RoomImage> images;

    // Helper method để lấy Hotel ID nhanh mà không bị lỗi "Cannot resolve getHotelId"
    public Long getHotelId() {
        return this.hotel != null ? this.hotel.getId() : null;
    }

    // Helper method để lấy RoomType ID nhanh
    public Long getRoomTypeId() {
        return this.roomType != null ? this.roomType.getId() : null;
    }
}