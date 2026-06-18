package com.ssafy.layover.place;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor
public class Place {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "original_category_code", length = 20)
    private String originalCategoryCode;

    @Column(length = 300)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "tour_api_id", length = 50)
    private String tourApiId;

    @Column(name = "content_type_id", length = 10)
    private String contentTypeId;

    @Column(name = "operating_hours", length = 500)
    private String operatingHours;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    // operating_hours 파싱이 비정형이므로 임시로 9~21시 영업 중 처리
    public boolean isCurrentlyOpen() {
        int hour = LocalDateTime.now().getHour();
        return hour >= 9 && hour < 21;
    }
}
