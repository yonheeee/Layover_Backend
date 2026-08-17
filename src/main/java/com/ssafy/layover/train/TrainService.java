package com.ssafy.layover.train;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TrainService {

    private final KorailApiClient korailApiClient;
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{8}");

    private static final Map<String, String> STATION_CODES = Map.of(
            "daejeon", "대전",
            "seo-daejeon", "서대전"
    );

    public List<TrainResponse> getTrains(String station, String date) {
        if (date == null || !DATE_PATTERN.matcher(date).matches()) {
            throw new IllegalArgumentException("열차 조회 날짜는 yyyyMMdd 형식이어야 합니다.");
        }
        String stnCd = STATION_CODES.get(station);
        if (stnCd == null) return List.of();
        return korailApiClient.fetchTrains(stnCd, date);
    }
}
