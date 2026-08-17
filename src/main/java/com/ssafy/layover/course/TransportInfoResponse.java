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
    private final String walkSource;
    private final String busSource;
    private final String taxiSource;
    private final String routePathSource;

    public TransportInfoResponse(String walkTime, String busTime, String taxiTime, int taxiFare) {
        this(walkTime, busTime, taxiTime, taxiFare, List.of());
    }

    public TransportInfoResponse(String walkTime, String busTime, String taxiTime, int taxiFare, List<double[]> routePath) {
        this(walkTime, busTime, taxiTime, taxiFare, routePath,
                "ESTIMATED", "UNAVAILABLE", "ESTIMATED", "STRAIGHT_LINE");
    }

    public TransportInfoResponse(String walkTime, String busTime, String taxiTime, int taxiFare,
                                 List<double[]> routePath, String walkSource, String busSource,
                                 String taxiSource, String routePathSource) {
        this.walkTime = walkTime;
        this.busTime = busTime;
        this.taxiTime = taxiTime;
        this.taxiFare = taxiFare;
        this.routePath = routePath != null ? routePath : List.of();
        this.walkSource = walkSource;
        this.busSource = busSource;
        this.taxiSource = taxiSource;
        this.routePathSource = routePathSource;
    }
}
