package com.ssafy.layover.course;

import com.ssafy.layover.place.Place;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "course_places")
@Getter
@NoArgsConstructor
public class CoursePlace {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "travel_time_min")
    private Integer travelTimeMin;

    @Builder
    public CoursePlace(Course course, Place place, int orderIndex, Integer travelTimeMin) {
        this.id = UUID.randomUUID().toString();
        this.course = course;
        this.place = place;
        this.orderIndex = orderIndex;
        this.travelTimeMin = travelTimeMin;
    }
}
