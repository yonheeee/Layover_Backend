package com.ssafy.layover.stamp;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StampMapper {

    void insert(Stamp stamp);

    List<Stamp> findByUserId(@Param("userId") String userId);

    /**
     * 같은 장소에 오늘 이미 스탬프를 찍었는지.
     *
     * <p>날짜 경계는 애플리케이션이 Asia/Seoul 기준으로 계산해서 넘긴다.
     * DATE(visited_at) = CURDATE() 로 쓰면 인덱스를 못 타고, CURDATE() 가
     * DB 세션 타임존을 따르므로 DB가 UTC일 때 하루 경계가 9시간 어긋난다.
     */
    boolean existsByUserIdAndPlaceIdBetween(@Param("userId") String userId,
                                            @Param("placeId") String placeId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);
}
