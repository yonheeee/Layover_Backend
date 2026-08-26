package com.ssafy.layover.course;

import java.util.List;

public record TransitStepResponse(
        String type,
        String guidance,
        int minutes,
        List<String> vehicles,
        List<String> stops
) {
    public TransitStepResponse {
        vehicles = vehicles != null ? List.copyOf(vehicles) : List.of();
        stops = stops != null ? List.copyOf(stops) : List.of();
    }
}
