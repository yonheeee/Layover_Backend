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
    private String restDate;
    private String infoCenter;
    private String parking;
    private String useFee;
    private String reservation;
    private boolean isActive;
    private String description;
    private String kakaoPlaceUrl;
    private String kakaoPhone;
    private String roadAddress;
    private String tourApiId;
    private String contentTypeId;
    private LocalDateTime syncedAt;

    /** OPEN / CLOSED / UNKNOWN. UNKNOWN이면 화면에서 "정보 확인 필요"로 안내한다. */
    private String openStatus;

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
                .restDate(place.getRestDate())
                .infoCenter(place.getInfoCenter())
                .parking(place.getParking())
                .useFee(place.getUseFee())
                .reservation(place.getReservation())
                .isActive(Boolean.TRUE.equals(place.getActive()))
                .description(place.getDescription())
                .kakaoPlaceUrl(place.getKakaoPlaceUrl())
                .kakaoPhone(place.getKakaoPhone())
                .roadAddress(place.getRoadAddress())
                .tourApiId(place.getTourApiId())
                .contentTypeId(place.getContentTypeId())
                .syncedAt(place.getSyncedAt())
                .openStatus(place.getOpenStatus().name())
                .build();
    }
}
