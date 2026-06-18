package com.ssafy.layover.course.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CourseResponse {
    private final String id;
    private final String title;
    private final String subTitle;
    private final String totalTime;
    private final String estimatedCost;
    private final List<CourseStopResponse> places;
}
