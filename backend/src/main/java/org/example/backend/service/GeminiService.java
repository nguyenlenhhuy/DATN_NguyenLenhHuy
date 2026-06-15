package org.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.dto.request.ChatRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final ChatContextService contextService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiService(WebClient.Builder webClientBuilder, ChatContextService contextService) {
        this.webClient = webClientBuilder.build();
        this.contextService = contextService;
    }

    // =========================================================
    // REST reactive — dùng cho /api/chat/send (không block reactor thread)
    // =========================================================
    public Mono<String> getChatbotResponse(ChatRequestDTO dto, Long userId) {
        String prompt = assemblePrompt(dto, userId);
        return webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildBody(prompt, dto))
                .retrieve()
                .bodyToMono(String.class)
                .map(raw -> {
                    String result = parseSync(raw);
                    return (result == null || result.isBlank()) ? fallback(dto.getMessage()) : result;
                })
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(3))
                        .filter(e -> e instanceof WebClientResponseException.TooManyRequests)
                        .maxBackoff(Duration.ofSeconds(10)))
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException wce && wce.getStatusCode().value() == 429) {
                        return Mono.just("Dạ, trợ lý AI đang bận do nhiều yêu cầu cùng lúc. Vui lòng thử lại sau 1–2 phút nhé!");
                    }
                    System.err.println("[GeminiService] Lỗi API: " + e.getMessage());
                    return Mono.just(fallback(dto.getMessage()));
                });
    }

    // =========================================================
    // SSE Streaming — dùng cho /api/chat/stream
    // Gọi Gemini streamGenerateContent với alt=sse,
    // mỗi dòng "data: {...}" được parse và emit ra client.
    // =========================================================
    public Flux<String> streamChatbotResponse(ChatRequestDTO dto, Long userId) {
        String prompt = assemblePrompt(dto, userId);
        String streamUrl = apiUrl.replace(":generateContent", ":streamGenerateContent");

        return webClient.post()
                .uri(streamUrl + "?key=" + apiKey + "&alt=sse")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildBody(prompt, dto))
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line != null && line.startsWith("data:"))
                .map(line -> line.substring(5).trim())
                .filter(data -> !data.isEmpty())
                .mapNotNull(this::extractChunkText)
                .filter(text -> !text.isEmpty())
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(3))
                        .filter(e -> e instanceof WebClientResponseException.TooManyRequests)
                        .maxBackoff(Duration.ofSeconds(10)))
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException wce && wce.getStatusCode().value() == 429) {
                        return Mono.just("Dạ, trợ lý AI đang bận do nhiều yêu cầu cùng lúc. Vui lòng thử lại sau 1–2 phút nhé!").flux();
                    }
                    System.err.println("[GeminiService] Lỗi stream: " + e.getMessage());
                    return getChatbotResponse(dto, userId)
                            .flux()
                            .onErrorReturn(fallback(dto.getMessage()));
                });
    }

    // =========================================================
    // Xây dựng prompt hoàn chỉnh = persona + HTML guide + context DB + câu hỏi
    // =========================================================
    private String assemblePrompt(ChatRequestDTO dto, Long userId) {
        String role = dto.getUserRole() != null ? dto.getUserRole().toUpperCase() : "GUEST";
        String context = contextService.buildContext(role, userId);
        String imageHint = dto.hasImage()
                ? "\n[Khách đã đính kèm ảnh. Hãy phân tích ảnh trong ngữ cảnh khách sạn và tư vấn phòng phù hợp.]\n"
                : "";
        return persona(role) + htmlGuide(role) + imageHint
                + "\n\n=== DỮ LIỆU THỰC TẾ HỆ THỐNG ===\n" + context
                + "\n=== CÂU HỎI ===\n" + dto.getMessage();
    }

    private String persona(String role) {
        return switch (role) {
            case "STAFF" -> """
                Bạn là trợ lý nghiệp vụ nội bộ của LuxeHotel, hỗ trợ nhân viên lễ tân và housekeeping.

                QUY TẮC ĐỊNH DẠNG (BẮT BUỘC — KHÔNG dùng markdown **text** hay *text*):
                - Dùng <b>chữ đậm</b> cho tiêu đề và thông tin quan trọng
                - Dùng <br> để xuống dòng
                - Dùng <ul><li>...</li></ul> cho danh sách

                Trả lời chính xác, chuyên nghiệp bằng tiếng Việt. Truy cập dữ liệu vận hành thực từ hệ thống.
                Quy định: Check-in 14:00, Check-out 12:00.
                """;
            case "ADMIN" -> """
                Bạn là công cụ báo cáo và hỗ trợ ra quyết định của quản trị viên LuxeHotel.

                QUY TẮC ĐỊNH DẠNG (BẮT BUỘC — KHÔNG dùng markdown **text** hay *text*):
                - Dùng <b>chữ đậm</b> cho tiêu đề mục và số liệu quan trọng
                - Dùng <br> để xuống dòng giữa các mục
                - Dùng <ul><li>...</li></ul> cho danh sách phòng hoặc thống kê

                Cung cấp số liệu đầy đủ, chi tiết từ dữ liệu vận hành thực. Giải thích ý nghĩa của từng chỉ số.
                Khi trả lời về giá phòng, liệt kê giá từng phòng cụ thể (số phòng + loại + giá).
                """;
            case "CUSTOMER" -> """
                Bạn là lễ tân AI cao cấp của LuxeHotel — người bạn đồng hành tin cậy cho khách đặt phòng.

                QUY TẮC ĐỊNH DẠNG (BẮT BUỘC — KHÔNG dùng markdown **text** hay *text*):
                - Dùng <b>chữ đậm</b> cho thông tin quan trọng
                - Dùng <br> để xuống dòng
                - Dùng <ul><li>...</li></ul> cho danh sách tiện ích, chính sách

                CÁCH TRẢ LỜI:
                - Xưng hô "Dạ" ở đầu câu, thân thiện và chu đáo
                - Trả lời ít nhất 3-5 câu, đầy đủ thông tin
                - Kết thúc bằng câu hỏi mở hoặc gợi ý hành động tiếp theo

                THÔNG TIN KHÁCH SẠN:
                - Check-in: <b>14:00</b> | Check-out: <b>12:00</b>
                - WiFi miễn phí tốc độ cao toàn khách sạn
                - Bữa sáng buffet: 6:30 – 10:00 (tầng 1)
                - Hồ bơi ngoài trời: 6:00 – 22:00 | Gym & Spa: 8:00 – 20:00
                - Hủy phòng miễn phí nếu hủy trước 24h check-in, sau đó tính phí 1 đêm đầu
                - Thanh toán: tiền mặt, thẻ ngân hàng, chuyển khoản, QR PayOS

                CHỈ đề xuất phòng có trong dữ liệu hệ thống.
                """;
            default -> """
                Bạn là lễ tân AI của LuxeHotel — hỗ trợ khách vãng lai tìm hiểu và đặt phòng.

                QUY TẮC ĐỊNH DẠNG (BẮT BUỘC — KHÔNG dùng markdown **text** hay *text*):
                - Dùng <b>chữ đậm</b> cho thông tin quan trọng
                - Dùng <br> để xuống dòng
                - Dùng <ul><li>...</li></ul> cho danh sách

                CÁCH TRẢ LỜI:
                - Thân thiện, chuyên nghiệp, xưng hô "Dạ" ở đầu câu
                - Trả lời ít nhất 3-5 câu, đầy đủ thông tin
                - Kết thúc bằng lời mời đặt phòng hoặc câu hỏi gợi mở

                THÔNG TIN KHÁCH SẠN:
                - Check-in: <b>14:00</b> | Check-out: <b>12:00</b>
                - WiFi miễn phí | Bữa sáng buffet 6:30–10:00 | Hồ bơi & Spa
                - Hủy phòng miễn phí trước 24h check-in
                - Thanh toán: tiền mặt, thẻ, chuyển khoản, QR PayOS

                CHỈ đề xuất phòng có trong dữ liệu hệ thống.
                """;
        };
    }

    private String htmlGuide(String role) {
        if ("STAFF".equals(role) || "ADMIN".equals(role)) return "";
        return """

            KHI KHÁCH HỎI TÌM PHÒNG HOẶC HỎI GIÁ PHÒNG, BẮT BUỘC tạo HTML card cho từng phòng phù hợp (KHÔNG bọc trong markdown ```html):

            <div class="room-card">
              <div class="room-info">
                <div class="room-name">Phòng [số phòng] – [loại phòng]</div>
                <div class="room-meta">Tầng [tầng] • Tối đa [số người] khách</div>
                <div class="room-price">[giá theo định dạng 1.500.000] VND/đêm</div>
                <a href="/rooms/[ID]" class="book-btn">🛏 Xem &amp; Đặt phòng</a>
              </div>
            </div>

            Hiển thị tối đa 4 phòng phù hợp nhất. Trước danh sách card, hãy giới thiệu ngắn gọn (1-2 câu).
            """;
    }

    // =========================================================
    // Rule-based fallback khi Gemini API lỗi
    // =========================================================
    private String fallback(String message) {
        if (message == null) return defaultError();
        String m = message.toLowerCase();
        if (m.contains("hủy") || m.contains("cancel"))
            return "Dạ, <b>chính sách hủy phòng</b> của LuxeHotel như sau:<br>" +
                   "<ul><li>Hủy <b>trước 24 giờ</b> trước check-in: <b>miễn phí hoàn toàn</b></li>" +
                   "<li>Hủy <b>trong vòng 24 giờ</b> trước check-in: tính phí <b>1 đêm đầu</b></li>" +
                   "<li>Không show: tính phí toàn bộ đặt phòng</li></ul>" +
                   "Bạn có cần hỗ trợ thêm gì không ạ?";
        if (m.contains("check-in") || m.contains("nhận phòng"))
            return "Dạ, giờ <b>nhận phòng (check-in)</b> tiêu chuẩn là <b>14:00 chiều</b>.<br>" +
                   "Nếu quý khách đến sớm hơn, chúng tôi sẽ cố gắng bố trí phòng sớm nhất có thể hoặc giữ hành lý miễn phí cho bạn trong thời gian chờ. <br>" +
                   "Quý khách có dự định đến vào khung giờ nào ạ?";
        if (m.contains("check-out") || m.contains("trả phòng"))
            return "Dạ, giờ <b>trả phòng (check-out)</b> tiêu chuẩn là <b>12:00 trưa</b>.<br>" +
                   "Nếu quý khách cần trả phòng muộn hơn, vui lòng liên hệ lễ tân <b>trước 1 ngày</b> để được sắp xếp (có thể phát sinh phụ phí tùy tình hình phòng).<br>" +
                   "Chúng tôi có thể hỗ trợ giữ hành lý miễn phí sau khi trả phòng.";
        if (m.contains("wifi") || m.contains("internet"))
            return "Dạ, LuxeHotel cung cấp <b>WiFi miễn phí tốc độ cao</b> cho tất cả khách tại mọi khu vực của khách sạn.<br>" +
                   "Mật khẩu WiFi sẽ được cung cấp ngay tại quầy lễ tân khi quý khách nhận phòng.<br>" +
                   "Kết nối ổn định, phù hợp cho cả làm việc và giải trí!";
        if (m.contains("giá") || m.contains("bao nhiêu") || m.contains("phí"))
            return "Dạ, <b>giá phòng tại LuxeHotel</b> dao động từ <b>500.000 – 2.000.000 VND/đêm</b> tùy theo loại phòng và thời điểm.<br>" +
                   "Để xem giá và tình trạng phòng cụ thể, vui lòng truy cập <a href='/rooms'>trang danh sách phòng</a> hoặc hỏi tôi \"Tìm phòng cho [số khách] người\" để được tư vấn chi tiết!";
        if (m.contains("đặt phòng") || m.contains("booking") || m.contains("đặt"))
            return "Dạ, để <b>đặt phòng tại LuxeHotel</b>, quý khách thực hiện theo các bước sau:<br>" +
                   "<ul><li>Bước 1: Vào <a href='/rooms'>trang danh sách phòng</a> và chọn phòng ưng ý</li>" +
                   "<li>Bước 2: Chọn ngày check-in và check-out</li>" +
                   "<li>Bước 3: Nhấn <b>\"Đặt phòng\"</b> và điền thông tin</li>" +
                   "<li>Bước 4: Thanh toán — hệ thống xác nhận <b>ngay lập tức!</b></li></ul>" +
                   "Hoặc bạn có thể cho tôi biết ngày và số khách để tôi tìm phòng phù hợp nhé?";
        if (m.contains("địa chỉ") || m.contains("ở đâu") || m.contains("vị trí"))
            return "Dạ, <b>LuxeHotel</b> tọa lạc tại <b>trung tâm thành phố</b>, thuận tiện di chuyển đến các điểm tham quan, trung tâm mua sắm và nhà hàng nổi tiếng.<br>" +
                   "Vị trí đắc địa giúp quý khách dễ dàng khám phá thành phố. Chúng tôi cũng hỗ trợ đặt xe đưa đón từ sân bay/ga tàu theo yêu cầu!";
        if (m.contains("ăn sáng") || m.contains("bữa sáng"))
            return "Dạ, LuxeHotel phục vụ <b>bữa sáng buffet đa dạng</b> hàng ngày từ <b>6:30 – 10:00</b> tại nhà hàng tầng 1.<br>" +
                   "Thực đơn phong phú với các món Á – Âu, bánh mì tươi, trái cây theo mùa và các loại đồ uống nóng/lạnh.<br>" +
                   "Quý khách có muốn biết thêm về gói phòng có bao gồm bữa sáng không ạ?";
        if (m.contains("hồ bơi") || m.contains("gym") || m.contains("spa"))
            return "Dạ, LuxeHotel có đầy đủ tiện ích cao cấp:<br>" +
                   "<ul><li>🏊 <b>Hồ bơi ngoài trời:</b> 6:00 – 22:00 (miễn phí cho khách lưu trú)</li>" +
                   "<li>💪 <b>Phòng Gym:</b> 8:00 – 20:00 với trang thiết bị hiện đại</li>" +
                   "<li>💆 <b>Spa & Massage:</b> 8:00 – 20:00 (đặt lịch trước 2 tiếng)</li></ul>" +
                   "Tất cả đều miễn phí cho khách lưu trú. Bạn có muốn biết thêm dịch vụ nào khác không ạ?";
        return defaultError();
    }

    private String defaultError() {
        return "Dạ, hệ thống AI đang tạm thời bận. Vui lòng thử lại sau ít phút hoặc liên hệ <b>hotline lễ tân</b> để được hỗ trợ trực tiếp ngay nhé!";
    }

    // =========================================================
    // Helpers
    // =========================================================
    private Map<String, Object> buildBody(String prompt, ChatRequestDTO dto) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        if (dto != null && dto.hasImage()) {
            String mimeType = dto.getImageMimeType() != null ? dto.getImageMimeType() : "image/jpeg";
            parts.add(Map.of("inlineData", Map.of(
                    "mimeType", mimeType,
                    "data", dto.getImageBase64()
            )));
        }

        return Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 2048)
        );
    }

    private String parseSync(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractChunkText(String jsonData) {
        try {
            JsonNode root = objectMapper.readTree(jsonData);
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) return "";
            JsonNode text = candidates.get(0).path("content").path("parts").get(0).path("text");
            return text.isMissingNode() ? "" : text.asText();
        } catch (Exception e) {
            return "";
        }
    }
}
