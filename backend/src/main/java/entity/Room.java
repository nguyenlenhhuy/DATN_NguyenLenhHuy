package entity;

import entity.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity @Table(name = "rooms", uniqueConstraints = {@UniqueConstraint(columnNames = {"hotel_id", "room_number"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "hotel_id") private Hotel hotel;
    @ManyToOne @JoinColumn(name = "room_type_id") private RoomType roomType;
    @Column(name = "room_number") private String roomNumber;
    private Integer floor;
    private RoomStatus status = RoomStatus.AVAILABLE;
}
