package com.ssafy.layover.course;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CourseRegenerateRequest extends CourseGenerateRequest {
    private List<CurrentPlace> currentPlaces;

    @Getter
    @NoArgsConstructor
    public static class CurrentPlace {
        private String id;
        private boolean locked;
    }
}
