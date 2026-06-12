package org.example.backend.controller;
import org.example.backend.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin("*") // Cho phép Angular gọi API không bị lỗi CORS
public class ChatController {

    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody String message) {
        String response = geminiService.getChatbotResponse(message);
        return ResponseEntity.ok(response);
    }
}