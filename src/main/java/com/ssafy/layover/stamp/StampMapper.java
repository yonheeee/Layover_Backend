package com.ssafy.layover.stamp;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StampMapper {

    void insert(Stamp stamp);

    List<Stamp> findByUserId(@Param("userId") String userId);

    boolean existsByUserIdAndPlaceId(@Param("userId") String userId, @Param("placeId") String placeId);
}
