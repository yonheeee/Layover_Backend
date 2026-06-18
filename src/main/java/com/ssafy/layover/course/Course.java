package com.ssafy.layover.course;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor
public class Course {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "departure_station", nullable = false, length = 20)
    private String departureStation;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "travel_mode", nullable = false, length = 10)
    private String travelMode;

    @Column(name = "weather_condition", length = 20)
    private String weatherCondition;

    // JSON 배열을 문자열로 저장 ex) ["FOOD","CAFE"]
    @Column(name = "theme_tags", columnDefinition = "JSON")
    private String themeTags;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<CoursePlace> coursePlaces = new ArrayList<>();

    @Builder
    public Course(String userId, String departureStation, int durationMinutes,
                  String travelMode, String weatherCondition, String themeTags) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.departureStation = departureStation;
        this.durationMinutes = durationMinutes;
        this.travelMode = travelMode;
        this.weatherCondition = weatherCondition;
        this.themeTags = themeTags;
        this.createdAt = LocalDateTime.now();
    }
}
