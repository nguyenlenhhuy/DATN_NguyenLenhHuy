package org.example.backend.controller;

import jakarta.validation.Valid;
import org.example.backend.dto.request.RoomBatchRequest;
import org.example.backend.dto.request.RoomRequest;
import org.example.backend.dto.request.RoomTypeRequest;
import org.example.backend.dto.response.CustomerRoomResponseDTO;
import org.example.backend.dto.response.RoomResponseDTO;
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

    // ĐÃ BỔ SUNG: API lấy toàn bộ danh sách loại phòng để đồng bộ với thẻ Select và Table cấu hình của Angular
    @GetMapping("/room-types")
    public ResponseEntity<List<RoomType>> getRoomTypes() {
        return ResponseEntity.ok(roomService.getRoomTypes());
    }

    @PostMapping("/room-types")
    public ResponseEntity<RoomType> createRoomType(@Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.ok(roomService.createRoomType(request));
    }

    @DeleteMapping("/room-types/{id}")
    public ResponseEntity<Map<String, String>> deleteRoomType(@PathVariable Long id) {
        roomService.deleteRoomType(id);
        // TỐI ƯU UX: Trả về cấu trúc JSON { "message": "..." } để Frontend Angular không bị lỗi parse text thô
        return ResponseEntity.ok(Collections.singletonMap("message", "Xóa phân loại phòng hệ thống thành công!"));
    }

    // ================= APIs PHÒNG VẬT LÝ (ADMIN & STAFF) =================

    @PostMapping("/rooms")
    public ResponseEntity<Room> createRoom(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.createRoom(request));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<Map<String, String>> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        roomService.updateRoomDetails(id, request);
        return ResponseEntity.ok(Collections.singletonMap("message", "Cập nhật thông tin phòng và ảnh đại diện thành công!"));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Map<String, String>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id); // Gọi xuống hàm xóa mềm (Soft Delete) của Service mới cập nhật
        return ResponseEntity.ok(Collections.singletonMap("message", "Xóa phòng thành công (Hệ thống đã chuyển dữ liệu vào kho lưu trữ ẩn)!"));
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

    // ================= APIs DÀNH CHO KHÁCH HÀNG (CUSTOMER APP) =================

    // TỐI ƯU ĐƯỜNG DẪN: Lấy danh sách phòng công khai hiển thị phía Client
    @GetMapping("/public/rooms")
    public ResponseEntity<List<CustomerRoomResponseDTO>> getRoomsForCustomer() {
        return ResponseEntity.ok(roomService.getRoomsForCustomer());
    }
}