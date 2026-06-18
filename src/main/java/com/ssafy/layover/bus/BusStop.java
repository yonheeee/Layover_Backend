package com.ssafy.layover.bus;

public record BusStop(
        String busStopId,
        String busStopNm,
        double lat,
        double lng
) {}
