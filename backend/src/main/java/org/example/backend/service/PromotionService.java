package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.PromotionRequestDTO;
import org.example.backend.dto.response.PromotionResponseDTO;
import org.example.backend.entity.Favorite;
import org.example.backend.entity.Promotion;
import org.example.backend.entity.Room;
import org.example.backend.repository.FavoriteRepository;
import org.example.backend.repository.PromotionRepository;
import org.example.backend.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final RoomRepository roomRepository;
    private final FavoriteRepository favoriteRepository;
    private final EmailService emailService;

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
        Promotion promotion = Promotion.builder()
                .code(request.getCode().toUpperCase().trim()) // Chuẩn hóa mã viết hoa toàn bộ
                .discountPercentage(request.getDiscountPercentage())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true) // Mặc định khi vừa phát hành sẽ ở trạng thái hoạt động ngay
                .build();

        promotionRepository.save(promotion);
    }

    /**
     * Nghiệp vụ bật/tắt trạng thái hoạt động của Voucher
     */
    @Transactional
    public void togglePromotionStatus(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mã khuyến mãi không tồn tại trên hệ thống!"));

        promotion.setIsActive(!promotion.getIsActive());
        promotionRepository.save(promotion);
    }

    /**
     * Nghiệp vụ 3: Kiểm tra tính hợp lệ của mã và tính toán số tiền được giảm giá
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String code, BigDecimal currentAmount) {
        String cleanCode = code.trim();

        // 1. Tìm kiếm mã voucher công khai đang hoạt động
        Promotion promotion = promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue(cleanCode)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại hoặc đã bị vô hiệu hóa!"));

        // 2. Đồng bộ kiểu dữ liệu LocalDate theo thực thể
        LocalDate now = LocalDate.now();

        if (now.isBefore(promotion.getStartDate())) {
            throw new RuntimeException("Mã giảm giá chưa đến thời gian áp dụng!");
        }
        if (now.isAfter(promotion.getEndDate())) {
            throw new RuntimeException("Mã giảm giá này đã hết hạn sử dụng!");
        }

        // 3. Tính toán số tiền được giảm theo %
        BigDecimal percentage = BigDecimal.valueOf(promotion.getDiscountPercentage());
        return currentAmount.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * 🔥 ĐÃ CẬP NHẬT CHUẨN UX/UI:
     * Gán trực tiếp mã ưu đãi cố định vào trường appliedPromotion của phòng
     * giúp lưu trữ trạng thái bền vững dưới Database phục vụ Angular hiển thị tag ngoài thẻ ảnh!
     */
    @Transactional
    public void applyPromotionToRoom(Long roomId, Long promotionId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin phòng yêu cầu với mã ID: " + roomId));

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá yêu cầu với mã ID: " + promotionId));

        // Kiểm tra điều kiện hạn dùng và trạng thái của mã ưu đãi trước khi chốt gán vào phòng
        if (promotion.getIsActive() != null && !promotion.getIsActive()) {
            throw new IllegalStateException("Chương trình ưu đãi này đã bị vô hiệu hóa.");
        }
        if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Chương trình ưu đãi này đã hết hạn sử dụng.");
        }

        // 🔥 THAY THẾ LOGIC: Lưu cố định chương trình ưu đãi hiện hành trực tiếp lên thuộc tính Room
        room.setAppliedPromotion(promotion);
        roomRepository.save(room);

        // Gửi email thông báo async cho tất cả user đã yêu thích phòng này
        List<Favorite> favorites = favoriteRepository.findByRoomId(roomId);
        if (!favorites.isEmpty()) {
            String roomNumber = room.getRoomNumber();
            String roomTypeName = room.getRoomType() != null ? room.getRoomType().getTypeName() : "Phòng khách sạn";
            String promoCode = promotion.getCode();
            Integer discountPct = promotion.getDiscountPercentage();
            LocalDate endDate = promotion.getEndDate();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (Favorite fav : favorites) {
                        if (fav.getUser() != null && fav.getUser().getEmail() != null) {
                            emailService.sendPromotionNotificationEmail(
                                    fav.getUser().getEmail(),
                                    fav.getUser().getFullName(),
                                    roomNumber,
                                    roomTypeName,
                                    promoCode,
                                    discountPct,
                                    endDate
                            );
                        }
                    }
                }
            });
        }
    }
}