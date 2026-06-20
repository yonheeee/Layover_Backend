package com.ssafy.layover.course;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SaveCourseRequest {

    private String departureStation;
    private int durationMinutes;
    private String travelMode;
    private String weatherCondition;
    private List<String> themeTags;
    private List<PlaceItem> places;

    @Getter
    @NoArgsConstructor
    public static class PlaceItem {
        private String placeId;
        private int orderIndex;
        private Integer travelTimeMin;
    }
}
