package com.ssafy.layover.stamp;

import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stamp {

    private String id;
    private String userId;
    private String placeId;
    private String placeName;
    private String photoUrl;
    private LocalDateTime visitedAt;

    public static Stamp create(String userId, String placeId, String photoUrl) {
        Stamp s = new Stamp();
        s.id = UUID.randomUUID().toString();
        s.userId = userId;
        s.placeId = placeId;
        s.photoUrl = photoUrl;
        // JVM 기본 타임존에 맡기면 배포 서버가 UTC일 때 9시간 어긋나고,
        // 하루 1회 판정과 visited_on 생성 컬럼이 함께 틀어진다.
        s.visitedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        return s;
    }
}
