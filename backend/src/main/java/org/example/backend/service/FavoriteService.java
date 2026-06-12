package org.example.backend.service;

import org.example.backend.dto.response.FavoriteResponseDTO;
import org.example.backend.dto.response.PromotionResponseDTO;
import org.example.backend.entity.Favorite;
import org.example.backend.entity.Room;
import org.example.backend.entity.User;
import org.example.backend.repository.FavoriteRepository;
import org.example.backend.repository.RoomRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, RoomRepository roomRepository, UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long roomId) {
        Optional<Favorite> favoriteOpt = favoriteRepository.findByUserIdAndRoomId(userId, roomId);

        if (favoriteOpt.isPresent()) {
            // Nếu đã tồn tại bản ghi -> Người dùng muốn BỎ YÊU THÍCH
            favoriteRepository.delete(favoriteOpt.get());
            return false;
        } else {
            // Nếu chưa tồn tại -> Người dùng muốn THÊM YÊU THÍCH
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found"));

            // Dùng khởi tạo chuẩn của Java thay vì Lombok Builder
            Favorite favorite = new Favorite();
            favorite.setUser(user);
            favorite.setRoom(room);

            favoriteRepository.save(favorite);
            return true; // Trả về true nghĩa là đã thêm thành công (Tim đỏ)
        }
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponseDTO> getFavoritesByUserId(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        return favorites.stream().map(fav -> {
            Room room = fav.getRoom();

            // 1. Lấy ảnh đầu tiên
            String firstImageUrl = "";
            if (room.getImages() != null && !room.getImages().isEmpty()) {
                firstImageUrl = room.getImages().get(0).getImageUrl();
            }

            // 2. FIX 0đ: Lấy giá từ Room, nếu null thì tự động lấy BasePrice từ RoomType
            Double actualPrice = room.getPrice();
            if (actualPrice == null && room.getRoomType() != null && room.getRoomType().getBasePrice() != null) {
                actualPrice = room.getRoomType().getBasePrice().doubleValue();
            }

            // 3. FIX MẤT KHUYẾN MÃI: Map đầy đủ dữ liệu Promotion
            PromotionResponseDTO promoDTO = null;
            if (room.getAppliedPromotion() != null) {
                var promoEntity = room.getAppliedPromotion();
                promoDTO = PromotionResponseDTO.builder()
                        .id(promoEntity.getId())
                        .code(promoEntity.getCode())
                        .discountPercentage(promoEntity.getDiscountPercentage())
                        .startDate(promoEntity.getStartDate()) // Đã bổ sung
                        .endDate(promoEntity.getEndDate())     // Đã bổ sung
                        .isActive(promoEntity.getIsActive())   // Đã bổ sung
                        .build();
            }

            FavoriteResponseDTO.RoomFavDTO roomDTO = FavoriteResponseDTO.RoomFavDTO.builder()
                    .roomId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .floor(room.getFloor())
                    .price(actualPrice) // Sử dụng giá đã fix ở trên
                    .status(room.getStatus() != null ? room.getStatus().name() : "AVAILABLE")
                    .typeName(room.getRoomType() != null ? room.getRoomType().getTypeName() : "Standard Room")
                    .maxGuests(room.getRoomType() != null ? room.getRoomType().getMaxOccupancy() : 2)
                    .imageUrl(firstImageUrl)
                    .appliedPromotion(promoDTO) // Trả lại object Khuyến mãi vào DTO
                    .build();

            return FavoriteResponseDTO.builder()
                    .id(fav.getId())
                    .room(roomDTO)
                    .build();
        }).collect(Collectors.toList());
    }
}