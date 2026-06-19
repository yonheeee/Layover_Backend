package com.ssafy.layover.place;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Place {

    private String id;
    private String name;
    private String category;
    private String originalCategoryCode;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String tourApiId;
    private String contentTypeId;
    private String operatingHours;
    private String imageUrl;
    private Boolean active;          // is_active 컬럼
    private LocalDateTime deletedAt;
    private LocalDateTime syncedAt;

    public boolean isCurrentlyOpen() {
        int hour = LocalDateTime.now().getHour();
        return hour >= 9 && hour < 21;
    }
}
