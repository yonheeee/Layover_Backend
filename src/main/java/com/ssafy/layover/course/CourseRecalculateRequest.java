package com.ssafy.layover.course;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CourseRecalculateRequest extends CourseGenerateRequest {
    private String title;
    private List<String> placeIds;
}
