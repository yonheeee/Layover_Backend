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
    private final List<double[]> walkRoutePath;
    private final List<double[]> busRoutePath;
    private final List<double[]> taxiRoutePath;
    private final int busFare;
    private final int busTransfers;
    private final String busRouteType;
    private final List<String> busVehicles;
    private final List<String> busStops;
    private final List<TransitStepResponse> busSteps;

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
        this.walkRoutePath = List.of();
        this.busRoutePath = List.of();
        this.taxiRoutePath = List.of();
        this.busFare = 0;
        this.busTransfers = -1;
        this.busRouteType = "";
        this.busVehicles = List.of();
        this.busStops = List.of();
        this.busSteps = List.of();
    }

    public TransportInfoResponse(String walkTime, String busTime, String taxiTime, int taxiFare,
                                 List<double[]> routePath, String walkSource, String busSource,
                                 String taxiSource, String routePathSource,
                                 List<double[]> walkRoutePath, List<double[]> busRoutePath,
                                 List<double[]> taxiRoutePath, int busFare, int busTransfers,
                                 String busRouteType, List<String> busVehicles, List<String> busStops,
                                 List<TransitStepResponse> busSteps) {
        this.walkTime = walkTime;
        this.busTime = busTime;
        this.taxiTime = taxiTime;
        this.taxiFare = taxiFare;
        this.routePath = routePath != null ? routePath : List.of();
        this.walkSource = walkSource;
        this.busSource = busSource;
        this.taxiSource = taxiSource;
        this.routePathSource = routePathSource;
        this.walkRoutePath = walkRoutePath != null ? walkRoutePath : List.of();
        this.busRoutePath = busRoutePath != null ? busRoutePath : List.of();
        this.taxiRoutePath = taxiRoutePath != null ? taxiRoutePath : List.of();
        this.busFare = busFare;
        this.busTransfers = busTransfers;
        this.busRouteType = busRouteType != null ? busRouteType : "";
        this.busVehicles = busVehicles != null ? busVehicles : List.of();
        this.busStops = busStops != null ? busStops : List.of();
        this.busSteps = busSteps != null ? busSteps : List.of();
    }
}
