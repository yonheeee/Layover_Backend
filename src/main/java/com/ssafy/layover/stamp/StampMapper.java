package com.ssafy.layover.stamp;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StampMapper {

    void insert(Stamp stamp);

    /**
     * 내가 찍은 스탬프 목록. 지도 핀 좌표까지 함께 내려준다.
     *
     * <p>엔티티가 아니라 조회 전용 DTO 를 돌려준다. stamps 에는 좌표가 없어
     * places 를 조인해야 하고, 엔티티를 그대로 쓰면 호출자 본인의 userId 까지
     * 응답에 실린다.
     */
    List<MyStampResponse> findMyStamps(@Param("userId") String userId);

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
