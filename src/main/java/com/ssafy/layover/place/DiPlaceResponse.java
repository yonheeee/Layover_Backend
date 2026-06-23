package com.ssafy.layover.place;

import lombok.Getter;

@Getter
public class DiPlaceResponse {

    private final String id;
    private final String name;
    private final String category;
    private final Boolean isOpen;
    private final String desc;
    private final String latLng;
    private final double lat;
    private final double lng;

    public DiPlaceResponse(Place place) {
        this.id = place.getId();
        this.name = place.getName();
        this.category = place.getCategory();
        this.isOpen = place.isCurrentlyOpen();
        this.desc = place.getAddress() != null ? place.getAddress() : "";
        this.lat = place.getLatitude() != null ? place.getLatitude().doubleValue() : 0;
        this.lng = place.getLongitude() != null ? place.getLongitude().doubleValue() : 0;
        this.latLng = String.format("%.4f, %.4f", this.lat, this.lng);
    }
}
