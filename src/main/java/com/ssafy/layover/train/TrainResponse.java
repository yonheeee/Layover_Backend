package com.ssafy.layover.train;

public record TrainResponse(
        String trainNo,
        String departTime,
        String arriveTime,
        String destination,
        String stationName,
        String stationCode,
        String mrntNm
) {}
