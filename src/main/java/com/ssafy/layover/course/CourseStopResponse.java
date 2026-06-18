package com.ssafy.layover.course;

import com.ssafy.layover.place.Place;
import lombok.Getter;

@Getter
public class CourseStopResponse {

    private final String id;
    private final String name;
    private final String category;
    private final boolean isOpen;
    private final String stayTime;
    private final boolean isLocked;
    private final double lat;
    private final double lng;

    private final TransportInfoResponse nextTransport;

    private final String transport;
    private final String transportTime;
    private final String taxiFare;

    public CourseStopResponse(Place place, String stayTime,
                              TransportInfoResponse nextTransport, String travelMode) {
        this.id = place.getId();
        this.name = place.getName();
        this.category = place.getCategory();
        this.isOpen = place.isCurrentlyOpen();
        this.stayTime = stayTime;
        this.isLocked = false;
        this.lat = place.getLatitude() != null ? place.getLatitude().doubleValue() : 0;
        this.lng = place.getLongitude() != null ? place.getLongitude().doubleValue() : 0;
        this.nextTransport = nextTransport;

        boolean isWalk = "WALK".equals(travelMode);
        this.transport = isWalk ? "walk" : "taxi";
        this.transportTime = nextTransport != null
                ? (isWalk ? nextTransport.getWalkTime() : nextTransport.getTaxiTime())
                : null;
        this.taxiFare = (nextTransport != null && !isWalk)
                ? String.format("%,d원", nextTransport.getTaxiFare())
                : null;
    }
}
