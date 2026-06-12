package org.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String getChatbotResponse(String customerMessage) {

        // 1. TÍCH HỢP DỮ LIỆU ĐỘNG (Bổ sung link ảnh Thumbnail)
        // Sử dụng đường dẫn tuyệt đối http://localhost:4200/rooms/...
        String availableRooms = "1. Phòng Standard (Giá: 500,000 VND) - Ảnh: https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=200 - Link đặt: http://localhost:4200/rooms/1 \n" +
                "2. Phòng Vip (Giá: 1,500,000 VND) - Ảnh: https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=200 - Link đặt: http://localhost:4200/rooms/5";

        // 2. KỸ THUẬT PROMPT ENGINEERING (Ép AI sinh mã HTML thẻ phòng)
        String systemContext = """
            Bạn là lễ tân AI cao cấp của khách sạn LuxeHotel.
            Quy định: Nhận phòng 14:00, trả phòng 12:00. Hủy miễn phí trước 24h.
            
            Danh sách phòng đang trống hiện tại:
            %s
            
            Yêu cầu BẮT BUỘC khi khách hỏi tìm phòng hoặc xem phòng:
            - CHỈ tư vấn các phòng có trong danh sách trên.
            - Phải trả lời một câu giới thiệu ngắn gọn, lịch sự.
            - Sau đó, BẮT BUỘC tạo ra thẻ thông tin phòng bằng đúng mã HTML dưới đây (thay thế thông tin trong ngoặc vuông bằng thông tin thực tế, KHÔNG bọc trong block code markdown ```html):
            
            <div class="room-card">
                <img src="[Link Ảnh]" alt="[Tên phòng]">
                <div class="room-info">
                    <div class="room-name">[Tên phòng]</div>
                    <div class="room-price">[Giá tiền]</div>
                    <a href="[Link đặt]" target="_blank" class="book-btn">Xem & Đặt phòng</a>
                </div>
            </div>
            
            Câu hỏi của khách: %s
            """.formatted(availableRooms, customerMessage);

        // 3. XÂY DỰNG JSON AN TOÀN
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", systemContext)
                        ))
                )
        );

        try {
            // 4. GỌI API
            String response = webClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseGeminiResponse(response);

        } catch (Exception e) {
            System.err.println("Lỗi khi gọi Gemini API: " + e.getMessage());
            return "Dạ hiện tại hệ thống kiểm tra phòng đang quá tải một chút. Anh/chị vui lòng liên hệ hotline để được hỗ trợ ngay lập tức nhé!";
        }
    }

    // Hàm bóc tách nội dung text từ chuỗi JSON trả về
    private String parseGeminiResponse(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);

            JsonNode textNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            return textNode.asText();

        } catch (Exception e) {
            System.err.println("Lỗi bóc tách JSON: " + e.getMessage());
            return "Xin lỗi, tôi không thể xử lý câu trả lời lúc này.";
        }
    }
}