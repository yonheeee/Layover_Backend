package com.ssafy.layover.bookmark;

import com.ssafy.layover.bookmark.dto.BookmarkResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookmarkMapper {

    void insert(@Param("userId") String userId, @Param("placeId") String placeId);

    void delete(@Param("userId") String userId, @Param("placeId") String placeId);

    List<BookmarkResponse> findByUserId(@Param("userId") String userId);

    boolean existsByUserIdAndPlaceId(@Param("userId") String userId, @Param("placeId") String placeId);
}
