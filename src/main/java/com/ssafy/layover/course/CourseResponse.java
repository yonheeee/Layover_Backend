package com.ssafy.layover.course;

import lombok.Getter;

import java.util.List;

@Getter
public class CourseResponse {
    private final String id;
    private final String title;
    private final String subTitle;
    private final String totalTime;
    private final String estimatedCost;
    private final List<CourseStopResponse> places;
    private final String recommendationReason;
    private final int timeBudgetMinutes;
    private final int estimatedTotalMinutes;
    private final int returnBufferMinutes;
    private final List<String> dataSources;
    private final boolean fallbackUsed;

    public CourseResponse(String id, String title, String subTitle, String totalTime,
                          String estimatedCost, List<CourseStopResponse> places) {
        this(id, title, subTitle, totalTime, estimatedCost, places,
                "", 0, 0, 0, List.of(), false);
    }

    public CourseResponse(String id, String title, String subTitle, String totalTime,
                          String estimatedCost, List<CourseStopResponse> places,
                          String recommendationReason, int timeBudgetMinutes,
                          int estimatedTotalMinutes, int returnBufferMinutes,
                          List<String> dataSources, boolean fallbackUsed) {
        this.id = id;
        this.title = title;
        this.subTitle = subTitle;
        this.totalTime = totalTime;
        this.estimatedCost = estimatedCost;
        this.places = places;
        this.recommendationReason = recommendationReason;
        this.timeBudgetMinutes = timeBudgetMinutes;
        this.estimatedTotalMinutes = estimatedTotalMinutes;
        this.returnBufferMinutes = returnBufferMinutes;
        this.dataSources = dataSources != null ? dataSources : List.of();
        this.fallbackUsed = fallbackUsed;
    }
}
