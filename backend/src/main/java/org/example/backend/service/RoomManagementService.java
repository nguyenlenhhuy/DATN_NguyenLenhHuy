package org.example.backend.service;


import lombok.RequiredArgsConstructor;
import org.example.backend.entity.Room;
import org.example.backend.entity.RoomType;
import org.example.backend.entity.enums.RoomStatus;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.RoomRepository;
import org.example.backend.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomManagementService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    // ================= QUẢN LÝ LOẠI PHÒNG (ADMIN) =================

    @Transactional
    public RoomType createRoomType(RoomType roomType) {
        return roomTypeRepository.save(roomType);
    }

    @Transactional
    public void deleteRoomType(Long id) {
        if (!roomTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy loại phòng ID: " + id);
        }
        // Kiểm tra xem có phòng nào thuộc loại này không
        if (roomRepository.existsByRoomTypeId(id)) {
            throw new AppException("Không thể xóa loại phòng đang có phòng hoạt động!", HttpStatus.BAD_REQUEST);
        }
        roomTypeRepository.deleteById(id);
    }

    // ================= QUẢN LÝ PHÒNG (ADMIN) =================

    @Transactional
    public Room createRoom(Room room) {
        // Kiểm tra trùng số phòng trong cùng khách sạn
        if (roomRepository.existsByHotelIdAndRoomNumber(room.getHotelId(), room.getRoomNumber())) {
            throw new AppException("Số phòng " + room.getRoomNumber() + " đã tồn tại!", HttpStatus.BAD_REQUEST);
        }
        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ID: " + id));

        if ("OCCUPIED".equals(room.getStatus())) {
            throw new AppException("Không thể xóa phòng đang có khách ở!", HttpStatus.BAD_REQUEST);
        }
        roomRepository.deleteById(id);
    }

    // ================= CẬP NHẬT TRẠNG THÁI (STAFF & ADMIN) =================

    @Transactional
    public void updateRoomStatus(Long id, String status) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ID: " + id));

        try {
            // Chuyển đổi String thành Enum RoomStatus (Ví dụ: "AVAILABLE" -> RoomStatus.AVAILABLE)
            RoomStatus roomStatus = RoomStatus.valueOf(status.toUpperCase());
            room.setStatus(roomStatus);
        } catch (IllegalArgumentException e) {
            // Bắt lỗi nếu Frontend truyền lên một chuỗi không tồn tại trong Enum
            throw new AppException("Trạng thái phòng '" + status + "' không hợp lệ!", HttpStatus.BAD_REQUEST);
        }

        roomRepository.save(room);
    }
}