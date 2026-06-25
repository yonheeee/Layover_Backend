package com.ssafy.layover.course;

import lombok.Getter;

import java.util.List;

@Getter
public class TransportInfoResponse {
    private final String walkTime;
    private final String busTime;
    private final String taxiTime;
    private final int taxiFare;
    private final List<double[]> routePath;

    public TransportInfoResponse(String walkTime, String busTime, String taxiTime, int taxiFare) {
        this(walkTime, busTime, taxiTime, taxiFare, List.of());
    }

    public TransportInfoResponse(String walkTime, String busTime, String taxiTime, int taxiFare, List<double[]> routePath) {
        this.walkTime = walkTime;
        this.busTime = busTime;
        this.taxiTime = taxiTime;
        this.taxiFare = taxiFare;
        this.routePath = routePath != null ? routePath : List.of();
    }
}
