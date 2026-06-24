package com.ssafy.layover.place;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlaceMapper {

    List<Place> findAllWithLocation();

    List<Place> findByCategoryIn(@Param("categories") List<String> categories);

    List<Place> searchByKeyword(@Param("keyword") String keyword);

    Place findById(@Param("id") String id);

    List<Place> findAll(@Param("category") String category,
                        @Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

    int countAll(@Param("category") String category,
                 @Param("keyword") String keyword);

    void upsertPlace(Place place);

    void upsertStationPlace(Place place);
}
