package com.ssafy.layover.course.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CourseGenerateRequest {
    private String departureStation;  // DAEJEON, SEODDAEJEON
    private int durationMinutes;      // 60, 120, 180
    private String travelMode;        // WALK, TAXI
    private String weatherCondition;  // SUNNY, RAINY, CLOUDY (optional)
    private List<String> themeTags;   // FOOD, CAFE, NATURE, CULTURE, TOUR
}
