package com.ssafy.layover.place;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlaceMapper {

    List<Place> findAllWithLocation();

    List<Place> findByCategoryIn(@Param("categories") List<String> categories);

    List<Place> searchByKeyword(@Param("keyword") String keyword);
}
