package com.ssafy.layover.course.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransportInfoResponse {
    private final String walkTime;
    private final String busTime;
    private final String taxiTime;
    private final int taxiFare;
}
