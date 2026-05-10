package org.example.backend.controller;

import org.example.backend.dto.response.RoomResponseDTO;
import org.example.backend.dto.request.RoomSearchRequest;
import org.example.backend.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin("*")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<RoomResponseDTO>> searchRooms(RoomSearchRequest request) {
        return ResponseEntity.ok(roomService.searchRooms(request));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<RoomResponseDTO>> getFeaturedRooms() {
        return ResponseEntity.ok(roomService.getFeaturedRooms());
    }
}