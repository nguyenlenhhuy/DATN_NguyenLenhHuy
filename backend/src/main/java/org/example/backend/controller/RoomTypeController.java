package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.response.RoomTypeDetailResponse;
import org.example.backend.service.RoomTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @GetMapping("/{id}")
    public ResponseEntity<RoomTypeDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(roomTypeService.getRoomTypeDetail(id));
    }
}