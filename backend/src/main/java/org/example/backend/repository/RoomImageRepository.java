package org.example.backend.repository;

import jakarta.transaction.Transactional;
import org.example.backend.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM RoomImage r WHERE r.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
}