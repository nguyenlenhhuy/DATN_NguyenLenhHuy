package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.PromotionRequestDTO;
import org.example.backend.dto.response.PromotionResponseDTO;
import org.example.backend.entity.Promotion;
import org.example.backend.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    /**
     * Nghiệp vụ 1: Lấy toàn bộ danh sách mã khuyến mãi hệ thống đổ lên bảng Angular
     */
    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(p -> PromotionResponseDTO.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .discountPercentage(p.getDiscountPercentage())
                        .startDate(p.getStartDate())
                        .endDate(p.getEndDate())
                        .isActive(p.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Nghiệp vụ 2: Tiếp nhận dữ liệu từ form Angular và phát hành mã Voucher mới
     */
    @Transactional
    public void createPromotion(PromotionRequestDTO request) {
        // Kiểm tra xem mã code này đã tồn tại trong hệ thống chưa để tránh trùng lặp dữ liệu độc bản
        // (Nếu cần bạn có thể bổ sung thêm logic validation tại đây)

        Promotion promotion = Promotion.builder()
                .code(request.getCode().toUpperCase().trim()) // Chuẩn hóa mã viết hoa toàn bộ
                .discountPercentage(request.getDiscountPercentage())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true) // Mặc định khi vừa phát hành sẽ ở trạng thái hoạt động ngay
                .build();

        promotionRepository.save(promotion);
    }
    @Transactional
    public void togglePromotionStatus(Long id) {
        // Tìm kiếm voucher trong DB, nếu không thấy thì ném lỗi lập tức
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mã khuyến mãi không tồn tại trên hệ thống!"));

        // Đảo ngược trạng thái vật lý (Đang true thành false, đang false thành true)
        promotion.setIsActive(!promotion.getIsActive());

        // Lưu lại sự thay đổi xuống MySQL
        promotionRepository.save(promotion);
    }
}