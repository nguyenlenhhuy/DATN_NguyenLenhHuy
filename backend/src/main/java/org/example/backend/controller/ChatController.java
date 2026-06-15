package org.example.backend.controller;

import org.example.backend.dto.request.ChatRequestDTO;
import org.example.backend.repository.UserRepository;
import org.example.backend.service.GeminiService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GeminiService geminiService;
    private final UserRepository userRepository;

    public ChatController(GeminiService geminiService, UserRepository userRepository) {
        this.geminiService = geminiService;
        this.userRepository = userRepository;
    }

    /**
     * REST reactive — fallback cho môi trường không hỗ trợ SSE.
     */
    @PostMapping("/send")
    public Mono<ResponseEntity<String>> sendMessage(
            @RequestBody ChatRequestDTO dto,
            Principal principal) {
        Long userId = resolveUserId(principal);
        return geminiService.getChatbotResponse(dto, userId)
                .map(ResponseEntity::ok);
    }

    /**
     * SSE Streaming — frontend dùng fetch + ReadableStream để nhận từng chunk.
     * Spring MVC + WebFlux mix: trả Flux<String> với text/event-stream.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(
            @RequestBody ChatRequestDTO dto,
            Principal principal) {
        Long userId = resolveUserId(principal);
        return geminiService.streamChatbotResponse(dto, userId);
    }

    /** Lấy userId từ JWT nếu đã đăng nhập; trả null nếu là khách vãng lai. */
    private Long resolveUserId(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByUsernameOrEmail(principal.getName(), principal.getName())
                .map(u -> u.getId())
                .orElse(null);
    }
}
