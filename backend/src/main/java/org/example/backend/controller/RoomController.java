package org.example.backend.controller;

import org.example.backend.dto.response.RoomResponseDTO;
import org.example.backend.dto.request.RoomSearchRequest;
import org.example.backend.dto.response.RoomTypeDetailResponse;
import org.example.backend.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms") // Đường dẫn chuẩn cho khách hàng
@CrossOrigin("*")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // BỔ SUNG: Khách hàng xem chi tiết một phòng (Giải quyết lỗi 404 khi Angular gọi chi tiết)
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponseDTO> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    // Khách hàng tìm kiếm phòng
    @GetMapping("/search")
    public ResponseEntity<List<RoomResponseDTO>> searchRooms(RoomSearchRequest request) {
        return ResponseEntity.ok(roomService.searchRooms(request));
    }

    // Khách hàng xem phòng nổi bật
    @GetMapping("/featured")
    public ResponseEntity<List<RoomResponseDTO>> getFeaturedRooms() {
        return ResponseEntity.ok(roomService.getFeaturedRooms());
    }

    // Khách hàng xem danh sách loại phòng
    @GetMapping("/types")
    public ResponseEntity<List<RoomTypeDetailResponse>> getAllRoomTypes() {
        return ResponseEntity.ok(roomService.getAllRoomTypeDTOs());
    }

    // Khách hàng xem tất cả các phòng
    @GetMapping("/all")
    public ResponseEntity<List<RoomResponseDTO>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }
}