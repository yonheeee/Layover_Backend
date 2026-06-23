package com.ssafy.layover.tmap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TMapApiClient {

    private static final String WALK_URL = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";
    private static final String CAR_URL  = "https://apis.openapi.sk.com/tmap/routes?version=1";

    private final RestTemplate restTemplate;

    @Value("${tmap.api.key:}")
    private String apiKey;

    // 도보 이동시간 (분) 반환. 실패 시 -1
    public int getWalkMinutes(double fromLat, double fromLng, double toLat, double toLng) {
        if (haversineMeters(fromLat, fromLng, toLat, toLng) <= 100) {
            double distKm = haversineMeters(fromLat, fromLng, toLat, toLng) / 1000.0;
            return Math.max(1, (int) Math.round(distKm / 4.0 * 60));
        }
        try {
            Map<String, Object> body = buildBody(fromLat, fromLng, toLat, toLng, "출발지", "도착지");
            Map<?, ?> response = post(WALK_URL, body);
            int totalSeconds = extractTotalTime(response);
            return totalSeconds > 0 ? (int) Math.ceil(totalSeconds / 60.0) : -1;
        } catch (Exception e) {
            log.warn("T맵 도보 API 호출 실패: {}", e.getMessage());
            return -1;
        }
    }

    // 자동차(택시) 이동시간(분) + 요금(원) 반환. 실패 시 {-1, -1}
    public int[] getCarRouteInfo(double fromLat, double fromLng, double toLat, double toLng) {
        if (haversineMeters(fromLat, fromLng, toLat, toLng) <= 100) {
            return new int[]{1, 3800};
        }
        try {
            Map<String, Object> body = buildBody(fromLat, fromLng, toLat, toLng, "출발지", "도착지");
            Map<?, ?> response = post(CAR_URL, body);
            int totalSeconds = extractTotalTime(response);
            int taxiFare = extractTaxiFare(response);
            int minutes = totalSeconds > 0 ? (int) Math.ceil(totalSeconds / 60.0) : -1;
            return new int[]{minutes, taxiFare};
        } catch (Exception e) {
            log.warn("T맵 자동차 API 호출 실패: {}", e.getMessage());
            return new int[]{-1, -1};
        }
    }

    private Map<String, Object> buildBody(double fromLat, double fromLng,
                                           double toLat, double toLng,
                                           String startName, String endName) {
        // T맵은 X=경도, Y=위도 순서
        return Map.of(
                "startX", String.valueOf(fromLng),
                "startY", String.valueOf(fromLat),
                "endX",   String.valueOf(toLng),
                "endY",   String.valueOf(toLat),
                "reqCoordType",  "WGS84GEO",
                "resCoordType",  "WGS84GEO",
                "startName", startName,
                "endName",   endName
        );
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> post(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("appKey", apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
        return response.getBody();
    }

    // features 배열에서 totalTime 추출 (초 단위)
    @SuppressWarnings("unchecked")
    private int extractTotalTime(Map<?, ?> response) {
        if (response == null) return -1;
        List<?> features = (List<?>) response.get("features");
        if (features == null || features.isEmpty()) return -1;

        for (Object f : features) {
            Map<?, ?> feature = (Map<?, ?>) f;
            Map<?, ?> props = (Map<?, ?>) feature.get("properties");
            if (props == null) continue;
            Object totalTime = props.get("totalTime");
            if (totalTime instanceof Number) {
                return ((Number) totalTime).intValue();
            }
        }
        return -1;
    }

    // features 배열에서 taxiFare 추출 (원 단위)
    @SuppressWarnings("unchecked")
    private int extractTaxiFare(Map<?, ?> response) {
        if (response == null) return 0;
        List<?> features = (List<?>) response.get("features");
        if (features == null || features.isEmpty()) return 0;

        for (Object f : features) {
            Map<?, ?> feature = (Map<?, ?>) f;
            Map<?, ?> props = (Map<?, ?>) feature.get("properties");
            if (props == null) continue;
            Object fare = props.get("taxiFare");
            if (fare instanceof Number) {
                return ((Number) fare).intValue();
            }
        }
        return 0;
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
