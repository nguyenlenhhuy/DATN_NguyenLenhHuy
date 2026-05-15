package org.example.backend.controller;

import org.example.backend.entity.Room;
import org.example.backend.entity.RoomType;
import org.example.backend.service.RoomManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/management")
public class RoomManagementController {

    @Autowired
    private RoomManagementService roomService;

    // --- APIs dành cho Loại phòng ---
    @PostMapping("/room-types")
    public ResponseEntity<RoomType> createRoomType(@RequestBody RoomType roomType) {
        return ResponseEntity.ok(roomService.createRoomType(roomType));
    }

    @DeleteMapping("/room-types/{id}")
    public ResponseEntity<String> deleteRoomType(@PathVariable Long id) {
        roomService.deleteRoomType(id);
        return ResponseEntity.ok("Xóa loại phòng thành công");
    }

    // --- APIs dành cho Phòng ---
    @PostMapping("/rooms")
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        return ResponseEntity.ok(roomService.createRoom(room));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<String> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok("Xóa phòng thành công");
    }

    // --- API cập nhật trạng thái (Dành cho cả Staff) ---
    @PatchMapping("/rooms/{id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        roomService.updateRoomStatus(id, status);
        return ResponseEntity.ok("Cập nhật trạng thái phòng sang " + status + " thành công");
    }
}