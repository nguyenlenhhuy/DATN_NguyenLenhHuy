package org.example.backend.controller;

import jakarta.validation.Valid;
import org.example.backend.dto.request.RoomRequest;
import org.example.backend.dto.request.RoomTypeRequest;
import org.example.backend.dto.request.RoomBatchRequest; // Đã tối ưu hóa import trực tiếp
import org.example.backend.dto.response.RoomResponseDTO;
import org.example.backend.dto.response.CustomerRoomResponseDTO;
import org.example.backend.entity.Room;
import org.example.backend.entity.RoomType;
import org.example.backend.service.RoomManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/management")
@CrossOrigin(origins = "*") // Giải quyết triệt để lỗi chặn kết nối CORS từ ứng dụng Angular
public class RoomManagementController {

    @Autowired
    private RoomManagementService roomService;

    // ================= APIs LOẠI PHÒNG (ADMIN) =================

    @PostMapping("/room-types")
    public ResponseEntity<RoomType> createRoomType(@Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.ok(roomService.createRoomType(request));
    }

    @DeleteMapping("/room-types/{id}")
    public ResponseEntity<Map<String, String>> deleteRoomType(@PathVariable Long id) {
        roomService.deleteRoomType(id);
        // TỐI ƯU UX: Trả về cấu trúc JSON { "message": "..." } để Frontend Angular không bị lỗi parse text thô
        return ResponseEntity.ok(Collections.singletonMap("message", "Xóa loại phòng thành công"));
    }

    // ================= APIs PHÒNG VẬT LÝ (ADMIN & STAFF) =================

    @PostMapping("/rooms")
    public ResponseEntity<Room> createRoom(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.createRoom(request));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Map<String, String>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Xóa phòng thành công"));
    }

    @PatchMapping("/rooms/{id}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        roomService.updateRoomStatus(id, status);
        return ResponseEntity.ok(Collections.singletonMap("message", "Cập nhật trạng thái phòng sang " + status + " thành công"));
    }

    // API phục vụ vẽ sơ đồ lưới phòng trực quan cho lễ tân (Staff/Admin)
    @GetMapping("/hotels/{hotelId}/rooms")
    public ResponseEntity<List<RoomResponseDTO>> getRoomsByHotel(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomService.getRoomsByHotel(hotelId));
    }

    // API KHỞI TẠO PHÒNG HÀNG LOẠT KÈM ALBUM ẢNH MẪU
    @PostMapping("/rooms/batch")
    public ResponseEntity<Map<String, String>> createBulkRooms(@Valid @RequestBody RoomBatchRequest request) {
        roomService.createBulkRooms(request);
        return ResponseEntity.ok(Collections.singletonMap("message", "Khởi tạo hàng loạt hàng phòng kèm album ảnh thành công!"));
    }

    // ================= APIs DÀNH CHO KHÁCH HÀNG (CUSTOMERAPP) =================

    // TỐI ƯU ĐƯỜNG DẪN: Tách biệt hẳn luồng xem của Khách hàng, tránh đi qua bộ lọc quyền hạn của phân hệ Quản lý (management)
    @GetMapping("/public/rooms")
    public ResponseEntity<List<CustomerRoomResponseDTO>> getRoomsForCustomer() {
        return ResponseEntity.ok(roomService.getRoomsForCustomer());
    }
    @PutMapping("/rooms/{id}")
    public ResponseEntity<Map<String, String>> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        roomService.updateRoomDetails(id, request);
        return ResponseEntity.ok(java.util.Collections.singletonMap("message", "Cập nhật thông tin phòng và ảnh đại diện thành công!"));
    }
}