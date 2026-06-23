package com.ssafy.layover.place;

import lombok.Getter;

@Getter
public class PlaceSearchResponse {

    private final String id;
    private final String name;
    private final String category;
    private final Boolean isOpen;
    private final double lat;
    private final double lng;

    public PlaceSearchResponse(Place place) {
        this.id = place.getId();
        this.name = place.getName();
        this.category = place.getCategory();
        this.isOpen = place.isCurrentlyOpen();
        this.lat = place.getLatitude() != null ? place.getLatitude().doubleValue() : 0;
        this.lng = place.getLongitude() != null ? place.getLongitude().doubleValue() : 0;
    }
}
