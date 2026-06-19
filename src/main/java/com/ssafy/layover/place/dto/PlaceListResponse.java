package com.ssafy.layover.place.dto;

import com.ssafy.layover.place.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlaceListResponse {

    private String id;
    private String name;
    private String category;
    private String address;
    private double latitude;
    private double longitude;
    private String imageUrl;
    private String operatingHours;
    private boolean isActive;

    public static PlaceListResponse from(Place place) {
        return PlaceListResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .category(place.getCategory())
                .address(place.getAddress())
                .latitude(place.getLatitude() != null ? place.getLatitude().doubleValue() : 0)
                .longitude(place.getLongitude() != null ? place.getLongitude().doubleValue() : 0)
                .imageUrl(place.getImageUrl())
                .operatingHours(place.getOperatingHours())
                .isActive(Boolean.TRUE.equals(place.getActive()))
                .build();
    }
}
