package com.ssafy.layover.stamp;

import lombok.*;

import java.time.LocalDateTime;
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
        s.visitedAt = LocalDateTime.now();
        return s;
    }
}
