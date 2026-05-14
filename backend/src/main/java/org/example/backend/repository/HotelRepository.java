package org.example.backend.repository;

import org.example.backend.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    @Modifying

    @Query(value = "UPDATE hotels SET total_reviews = total_reviews + 1, " +

            "avg_rating = (avg_rating * total_reviews + ?2) / (total_reviews + 1) " +

            "WHERE id = ?1", nativeQuery = true)

    void updateHotelRating(Long hotelId, Integer newRating);

}