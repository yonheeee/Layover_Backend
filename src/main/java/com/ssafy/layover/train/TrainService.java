package com.ssafy.layover.train;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainService {

    private final KorailApiClient korailApiClient;
    private final TagoTrainApiClient tagoTrainApiClient;
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{8}");

    @Value("${train.api.provider:tago}")
    private String trainApiProvider;

    private static final Map<String, String> KORAIL_STATION_NAMES = Map.of(
            "daejeon", "대전",
            "seo-daejeon", "서대전"
    );

    private static final Map<String, String> TAGO_STATION_CODES = Map.ofEntries(
            Map.entry("daejeon", "NAT011668"),
            Map.entry("seo-daejeon", "NAT030057"),
            Map.entry("sintanjin", "NAT011524"),
            Map.entry("seoul", "NAT010000"),
            Map.entry("yongsan", "NAT010032"),
            Map.entry("gwangmyeong", "NAT010045"),
            Map.entry("cheonan-asan", "NATH10960"),
            Map.entry("osong", "NAT050044"),
            Map.entry("dongdaegu", "NAT013271"),
            Map.entry("daegu", "NAT013239"),
            Map.entry("busan", "NAT014445"),
            Map.entry("ulsan", "NATH13717"),
            Map.entry("iksan", "NAT030879"),
            Map.entry("gwangju-songjeong", "NAT031857"),
            Map.entry("mokpo", "NAT032563")
    );

    public List<TrainResponse> getTrains(String station, String destination, String date) {
        if (date == null || !DATE_PATTERN.matcher(date).matches()) {
            throw new IllegalArgumentException("열차 조회 날짜는 yyyyMMdd 형식이어야 합니다.");
        }

        String provider = trainApiProvider == null ? "tago" : trainApiProvider.trim().toLowerCase();
        if ("korail-sample".equals(provider)) {
            return fetchKorailSample(station, date);
        }

        List<TrainResponse> tagoTrains = fetchTago(station, destination, date);
        if (!tagoTrains.isEmpty() || !"auto".equals(provider)) {
            return tagoTrains;
        }

        log.warn("[Train] TAGO returned no trains. Falling back to Korail sample API.");
        return fetchKorailSample(station, date);
    }

    private List<TrainResponse> fetchTago(String station, String destination, String date) {
        String departureCode = TAGO_STATION_CODES.get(station);
        String destinationCode = TAGO_STATION_CODES.get(destination);
        if (departureCode == null || destinationCode == null) {
            log.warn("[Train] TAGO station code is missing. station={} destination={}", station, destination);
            return List.of();
        }
        return tagoTrainApiClient.fetchTrains(departureCode, destinationCode, date);
    }

    private List<TrainResponse> fetchKorailSample(String station, String date) {
        String stationName = KORAIL_STATION_NAMES.get(station);
        if (stationName == null) return List.of();
        return korailApiClient.fetchTrains(stationName, date);
    }
}
