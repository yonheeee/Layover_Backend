package com.ssafy.layover.train;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrainService {

    private final KorailApiClient korailApiClient;

    private static final Map<String, String> STATION_CODES = Map.of(
            "daejeon", "대전",
            "seo-daejeon", "서대전"
    );

    public List<TrainResponse> getTrains(String station, String date) {
        String stnCd = STATION_CODES.get(station);
        if (stnCd == null) return List.of();
        return korailApiClient.fetchTrains(stnCd, date);
    }
}
