package org.example.backend.service;

import org.example.backend.entity.Favorite;
import org.example.backend.entity.Room;
import org.example.backend.entity.User;
import org.example.backend.repository.FavoriteRepository;
import org.example.backend.repository.RoomRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository; // Inject UserRepository của bạn vào đây

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
            return false; // Trả về false nghĩa là trạng thái hiện tại đã bỏ thích
        } else {
            // Nếu chưa tồn tại -> Người dùng muốn THÊM YÊU THÍCH
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found"));

            Favorite favorite = Favorite.builder()
                    .user(user)
                    .room(room)
                    .build();

            favoriteRepository.save(favorite);
            return true; // Trả về true nghĩa là đã thêm thành công (Tim đỏ)
        }
    }
}