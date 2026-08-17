package com.ssafy.layover.train;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class TagoTrainApiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${train.tago.base-url:https://apis.data.go.kr/1613000/TrainInfo/GetStrtpntAlocFndTrainInfo}")
    private String baseUrl;

    @Value("${train.tago.api-key:}")
    private String apiKey;

    @Value("${train.tago.train-grade-code:}")
    private String trainGradeCode;

    public List<TrainResponse> fetchTrains(String departureStationCode, String destinationStationCode, String date) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[TAGO Train] API key is not configured. Set TAGO_TRAIN_API_KEY.");
            return List.of();
        }
        if (departureStationCode == null || departureStationCode.isBlank()
                || destinationStationCode == null || destinationStationCode.isBlank()) {
            log.warn("[TAGO Train] station code is missing. dep={}, arr={}", departureStationCode, destinationStationCode);
            return List.of();
        }

        try {
            String uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("serviceKey", serviceKeyParam())
                    .queryParam("_type", "json")
                    .queryParam("numOfRows", 100)
                    .queryParam("pageNo", 1)
                    .queryParam("depPlaceId", departureStationCode)
                    .queryParam("arrPlaceId", destinationStationCode)
                    .queryParam("depPlandTime", date)
                    .build(false)
                    .toUriString();
            if (trainGradeCode != null && !trainGradeCode.isBlank()) {
                uri += "&trainGradeCode=" + URLEncoder.encode(trainGradeCode, StandardCharsets.UTF_8);
            }

            log.info("[TAGO Train] request dep={} arr={} date={}", departureStationCode, destinationStationCode, date);

            HttpURLConnection conn = (HttpURLConnection) URI.create(uri).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            String body = new String(
                    (status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream()).readAllBytes(),
                    StandardCharsets.UTF_8
            );

            if (status != 200) {
                log.warn("[TAGO Train] failed status={} body={}", status, abbreviate(body));
                return List.of();
            }

            Map<?, ?> root = objectMapper.readValue(body, Map.class);
            Map<?, ?> response = asMap(root.get("response"));
            Map<?, ?> header = response != null ? asMap(response.get("header")) : null;
            String resultCode = stringValue(header, "resultCode");
            if (resultCode != null && !resultCode.equals("00") && !resultCode.equals("0")) {
                log.warn("[TAGO Train] error resultCode={} resultMsg={}", resultCode, stringValue(header, "resultMsg"));
                return List.of();
            }

            Map<?, ?> responseBody = response != null ? asMap(response.get("body")) : null;
            Map<?, ?> items = responseBody != null ? asMap(responseBody.get("items")) : null;
            List<?> itemList = normalizeItems(items != null ? items.get("item") : null);

            return itemList.stream()
                    .map(this::toTrainResponse)
                    .filter(Objects::nonNull)
                    .filter(train -> train.trainNo() != null && !train.trainNo().isBlank())
                    .filter(train -> train.departTime() != null && !train.departTime().isBlank())
                    .collect(
                            LinkedHashMap<String, TrainResponse>::new,
                            (map, train) -> map.putIfAbsent(train.trainNo() + ":" + train.departTime(), train),
                            Map::putAll
                    )
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(TrainResponse::departTime))
                    .toList();
        } catch (Exception e) {
            log.warn("[TAGO Train] call failed: {}", e.getMessage());
            return List.of();
        }
    }

    private TrainResponse toTrainResponse(Object raw) {
        Map<?, ?> item = asMap(raw);
        if (item == null) return null;

        String trainNo = stringValue(item, "trainno");
        String trainName = stringValue(item, "traingradename");
        String departTime = toHhmm(stringValue(item, "depplandtime"));
        String arriveTime = toHhmm(stringValue(item, "arrplandtime"));
        String destination = stringValue(item, "arrplacename");
        String stationName = stringValue(item, "depplacename");

        return new TrainResponse(trainNo, departTime, arriveTime, destination, stationName, null, trainName);
    }

    private String serviceKeyParam() {
        if (apiKey.contains("%")) return apiKey;
        return URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
    }

    private String toHhmm(String yyyymmddhhmmss) {
        if (yyyymmddhhmmss == null || yyyymmddhhmmss.length() < 12) return "";
        return yyyymmddhhmmss.substring(8, 10) + ":" + yyyymmddhhmmss.substring(10, 12);
    }

    private String stringValue(Map<?, ?> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<?, ?>) value : null;
    }

    private List<?> normalizeItems(Object value) {
        if (value instanceof List<?>) return (List<?>) value;
        if (value instanceof Map<?, ?>) return List.of(value);
        return List.of();
    }

    private String abbreviate(String value) {
        if (value == null) return "";
        return value.substring(0, Math.min(value.length(), 240));
    }
}
