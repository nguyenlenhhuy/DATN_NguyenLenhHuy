package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    @JsonIgnoreProperties("rooms")
    private RoomType roomType;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    private Integer floor;

    @Column(name = "price")
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<RoomImage> images;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // Các phương thức hỗ trợ để lấy ID thay vì Object toàn phần
    public Long getHotelId() {
        return this.hotel != null ? this.hotel.getId() : null;
    }

    public Long getRoomTypeId() {
        return this.roomType != null ? this.roomType.getId() : null;
    }

    // Phương thức kiểm tra xóa để dùng trong Service
    public boolean isDeleted() {
        return this.isDeleted != null && this.isDeleted;
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_promotions",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "promotion_id")
    )
    private java.util.Set<Promotion> promotions = new java.util.HashSet<>();

    // ✔️ TRƯỜNG ĐÁNH DẤU CHƯƠNG TRÌNH ĐANG ÁP DỤNG THỰC TẾ LÊN PHÒNG
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_promotion_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "rooms"})
    private Promotion appliedPromotion;
}