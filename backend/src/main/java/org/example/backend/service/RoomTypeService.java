package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.response.RoomTypeDetailResponse;
import org.example.backend.entity.RoomType;
import org.example.backend.entity.RoomTypeImage;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeDetailResponse getRoomTypeDetail(Long id) {
        // 1. Lấy Entity từ DB
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng!"));

        // 2. Chuyển đổi (Map) Entity sang DTO
        return RoomTypeDetailResponse.builder()
                .id(roomType.getId())
                .typeName(roomType.getTypeName())
                .basePrice(roomType.getBasePrice())
                .maxOccupancy(roomType.getMaxOccupancy())
                .hotelName(roomType.getHotel().getName())
                .address(roomType.getHotel().getAddress())
                .imageUrls(roomType.getImages().stream()
                        .map(RoomTypeImage::getImageUrl)
                        .collect(Collectors.toList()))
                .build();
    }
}