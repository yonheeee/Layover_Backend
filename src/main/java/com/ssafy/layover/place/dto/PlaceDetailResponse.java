package com.ssafy.layover.place.dto;

import com.ssafy.layover.place.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PlaceDetailResponse {

    private String id;
    private String name;
    private String category;
    private String address;
    private double latitude;
    private double longitude;
    private String imageUrl;
    private String operatingHours;
    private boolean isActive;
    private String description;
    private String tourApiId;
    private String contentTypeId;
    private LocalDateTime syncedAt;

    public static PlaceDetailResponse from(Place place) {
        return PlaceDetailResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .category(place.getCategory())
                .address(place.getAddress())
                .latitude(place.getLatitude() != null ? place.getLatitude().doubleValue() : 0)
                .longitude(place.getLongitude() != null ? place.getLongitude().doubleValue() : 0)
                .imageUrl(place.getImageUrl())
                .operatingHours(place.getOperatingHours())
                .isActive(Boolean.TRUE.equals(place.getActive()))
                .description(place.getDescription())
                .tourApiId(place.getTourApiId())
                .contentTypeId(place.getContentTypeId())
                .syncedAt(place.getSyncedAt())
                .build();
    }
}
