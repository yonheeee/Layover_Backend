package com.ssafy.layover.course;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.layover.place.Place;
import lombok.Getter;

@Getter
public class CourseStopResponse {

    private final String id;
    private final String name;
    private final String category;
    @JsonProperty("isOpen")
    private final boolean isOpen;
    private final String stayTime;
    @JsonProperty("isLocked")
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

    private CourseStopResponse(String id, String name, String category, boolean isOpen,
                               String stayTime, boolean isLocked, double lat, double lng,
                               TransportInfoResponse nextTransport, String transport,
                               String transportTime, String taxiFare) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.isOpen = isOpen;
        this.stayTime = stayTime;
        this.isLocked = isLocked;
        this.lat = lat;
        this.lng = lng;
        this.nextTransport = nextTransport;
        this.transport = transport;
        this.transportTime = transportTime;
        this.taxiFare = taxiFare;
    }

    public static CourseStopResponse ofStation(String stationName, double lat, double lng,
                                               TransportInfoResponse nextTransport, String travelMode) {
        boolean isWalk = "WALK".equals(travelMode);
        String transport = isWalk ? "walk" : "taxi";
        String transportTime = nextTransport != null
                ? (isWalk ? nextTransport.getWalkTime() : nextTransport.getTaxiTime())
                : null;
        String taxiFare = (nextTransport != null && !isWalk)
                ? String.format("%,d원", nextTransport.getTaxiFare())
                : null;
        return new CourseStopResponse("__STATION__", stationName, "STATION", true, null, true,
                lat, lng, nextTransport, transport, transportTime, taxiFare);
    }
}
