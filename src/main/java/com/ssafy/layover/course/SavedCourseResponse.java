package com.ssafy.layover.course;

import com.ssafy.layover.place.Place;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class SavedCourseResponse {

    private String id;
    private String subTitle;
    private String travelMode;
    private int durationMinutes;
    private LocalDateTime createdAt;
    private List<PlaceSummary> places;

    @Getter
    @AllArgsConstructor
    public static class PlaceSummary {
        private String id;
        private String name;
        private String category;
        private double lat;
        private double lng;
    }

    public static SavedCourseResponse from(Course course, List<Place> places) {
        String subTitle = places.stream()
                .map(Place::getName)
                .collect(Collectors.joining(" → "));

        List<PlaceSummary> summaries = places.stream()
                .map(p -> new PlaceSummary(
                        p.getId(),
                        p.getName(),
                        p.getCategory(),
                        p.getLatitude() != null ? p.getLatitude().doubleValue() : 0,
                        p.getLongitude() != null ? p.getLongitude().doubleValue() : 0))
                .collect(Collectors.toList());

        return new SavedCourseResponse(
                course.getId(),
                subTitle,
                course.getTravelMode(),
                course.getDurationMinutes(),
                course.getCreatedAt(),
                summaries);
    }
}
