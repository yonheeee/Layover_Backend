package com.ssafy.layover.place;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, String> {

    List<Place> findByIsActiveTrueAndLatitudeIsNotNullAndLongitudeIsNotNull();

    List<Place> findByCategoryInAndIsActiveTrue(List<String> categories);

    @Query("SELECT p FROM Place p WHERE p.isActive = true AND p.latitude IS NOT NULL " +
           "AND (:keyword = '' OR p.name LIKE %:keyword%)")
    List<Place> searchByKeyword(@Param("keyword") String keyword);
}
