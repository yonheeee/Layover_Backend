package com.ssafy.layover.bus;

public record BusStopResponse(
        String id,
        String name,
        double lat,
        double lng
) {
    public static BusStopResponse from(BusStop stop) {
        return new BusStopResponse(stop.busStopId(), stop.busStopNm(), stop.lat(), stop.lng());
    }
}
