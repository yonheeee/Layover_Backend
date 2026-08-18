package com.ssafy.layover.course;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CourseGenerateRequest {
    private String departureStation;  // DAEJEON, SEODDAEJEON
    private int durationMinutes;      // 60, 120, 180
    private String travelMode;        // WALK, TAXI
    private String weatherCondition;
    private List<String> themeTags;   // FOOD, CAFE, NATURE, CULTURE, TOUR

    /**
     * 프론트엔드는 날씨를 채우지 않는다. 백엔드가 출발역 좌표로 조회해 채운 뒤
     * AI 프롬프트와 저장 코스에 함께 사용한다.
     */
    public void applyWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }
}
