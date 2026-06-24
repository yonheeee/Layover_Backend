package com.ssafy.layover.tmap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class TMapApiClient {

    private static final String WALK_URL = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";
    private static final String CAR_URL = "https://apis.openapi.sk.com/tmap/routes?version=1";
    private static final int MAX_ATTEMPTS = 3;

    private final RestTemplate restTemplate;
    private final AtomicBoolean blocked = new AtomicBoolean(false);

    @Value("${tmap.api.key:}")
    private String apiKey;

    public int getWalkMinutes(double fromLat, double fromLng, double toLat, double toLng) {
        if (isBlocked("도보")) {
            return -1;
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Map<String, Object> body = buildBody(fromLat, fromLng, toLat, toLng, "출발지", "도착지");
                Map<?, ?> response = post(WALK_URL, body);
                int totalSeconds = extractTotalTime(response);
                return totalSeconds > 0 ? (int) Math.ceil(totalSeconds / 60.0) : -1;
            } catch (Exception e) {
                log.warn("[TMap] 도보 API 호출 실패 {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }

        blockFurtherCalls("도보");
        return -1;
    }

    public int[] getCarRouteInfo(double fromLat, double fromLng, double toLat, double toLng) {
        if (isBlocked("자동차")) {
            return new int[]{-1, -1};
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Map<String, Object> body = buildBody(fromLat, fromLng, toLat, toLng, "출발지", "도착지");
                Map<?, ?> response = post(CAR_URL, body);
                int totalSeconds = extractTotalTime(response);
                int taxiFare = extractTaxiFare(response);
                int minutes = totalSeconds > 0 ? (int) Math.ceil(totalSeconds / 60.0) : -1;
                return new int[]{minutes, taxiFare};
            } catch (Exception e) {
                log.warn("[TMap] 자동차 API 호출 실패 {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }

        blockFurtherCalls("자동차");
        return new int[]{-1, -1};
    }

    private boolean isBlocked(String apiName) {
        if (!blocked.get()) {
            return false;
        }
        log.warn("[TMap] 실패했습니다 - 이전 {} API 실패로 추가 호출을 차단합니다.", apiName);
        return true;
    }

    private void blockFurtherCalls(String apiName) {
        if (blocked.compareAndSet(false, true)) {
            log.error("[TMap] 실패했습니다 - {} API {}회 시도 실패로 추가 호출을 차단합니다.", apiName, MAX_ATTEMPTS);
        }
    }

    private Map<String, Object> buildBody(double fromLat, double fromLng,
                                          double toLat, double toLng,
                                          String startName, String endName) {
        return Map.of(
                "startX", String.valueOf(fromLng),
                "startY", String.valueOf(fromLat),
                "endX", String.valueOf(toLng),
                "endY", String.valueOf(toLat),
                "reqCoordType", "WGS84GEO",
                "resCoordType", "WGS84GEO",
                "startName", startName,
                "endName", endName
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
}
