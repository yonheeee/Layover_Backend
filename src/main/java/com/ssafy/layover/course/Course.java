package com.ssafy.layover.course;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    private String id;
    private String userId;
    private String departureStation;
    private int durationMinutes;
    private String travelMode;
    private String weatherCondition;
    private String themeTags;        // JSON 배열 문자열
    private Boolean isPublic;        // is_public 컬럼
    private LocalDateTime createdAt;

    public static Course create(String userId, String departureStation, int durationMinutes,
                                String travelMode, String weatherCondition, String themeTags) {
        Course c = new Course();
        c.id = UUID.randomUUID().toString();
        c.userId = userId;
        c.departureStation = departureStation;
        c.durationMinutes = durationMinutes;
        c.travelMode = travelMode;
        c.weatherCondition = weatherCondition;
        c.themeTags = themeTags;
        c.isPublic = false;
        c.createdAt = LocalDateTime.now();
        return c;
    }
}
