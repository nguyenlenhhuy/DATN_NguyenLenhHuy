package org.example.backend.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReviewResponseDTO {
    private Long id;
    private String userName;
    private Integer rating;
    private String comment;
    private String replyContent;
    private LocalDateTime createdAt;
    private List<String> mediaUrls;
}