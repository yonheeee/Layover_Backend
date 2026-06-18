package com.ssafy.layover.course;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePlace {

    private String id;
    private String courseId;
    private String placeId;
    private int orderIndex;
    private Integer travelTimeMin;

    public static CoursePlace of(String courseId, String placeId, int orderIndex, Integer travelTimeMin) {
        CoursePlace cp = new CoursePlace();
        cp.id = UUID.randomUUID().toString();
        cp.courseId = courseId;
        cp.placeId = placeId;
        cp.orderIndex = orderIndex;
        cp.travelTimeMin = travelTimeMin;
        return cp;
    }
}
