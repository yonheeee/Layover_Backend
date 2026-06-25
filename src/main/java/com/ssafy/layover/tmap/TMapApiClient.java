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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TMapApiClient {

    private static final String WALK_URL = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";
    private static final String CAR_URL = "https://apis.openapi.sk.com/tmap/routes?version=1";

    private final RestTemplate restTemplate;

    @Value("${tmap.api.key:}")
    private String apiKey;

    public record WalkRouteResult(int minutes, List<double[]> path) {
        public static WalkRouteResult failed() { return new WalkRouteResult(-1, Collections.emptyList()); }
    }

    public record CarRouteResult(int minutes, int taxiFare, List<double[]> path) {
        public static CarRouteResult failed() { return new CarRouteResult(-1, -1, Collections.emptyList()); }
    }

    public WalkRouteResult getWalkRouteResult(double fromLat, double fromLng, double toLat, double toLng) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[TMap] API 키가 설정되지 않았습니다 (tmap.api.key)");
            return WalkRouteResult.failed();
        }
        try {
            log.info("[TMap] 도보경로 요청 ({},{}) → ({},{})", fromLat, fromLng, toLat, toLng);
            Map<String, Object> body = buildBody(fromLat, fromLng, toLat, toLng, "출발지", "도착지");
            Map<?, ?> response = post(WALK_URL, body);
            int totalSeconds = extractTotalTime(response);
            int minutes = totalSeconds > 0 ? (int) Math.ceil(totalSeconds / 60.0) : -1;
            List<double[]> path = extractPath(response);
            log.info("[TMap] 도보경로 완료 — {}분, 좌표{}개", minutes, path.size());
            return new WalkRouteResult(minutes, path);
        } catch (Exception e) {
            log.warn("[TMap] 도보 API 호출 실패: {}", e.getMessage());
            return WalkRouteResult.failed();
        }
    }

    public CarRouteResult getCarRouteResult(double fromLat, double fromLng, double toLat, double toLng) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[TMap] API 키가 설정되지 않았습니다 (tmap.api.key)");
            return CarRouteResult.failed();
        }
        try {
            log.info("[TMap] 자동차경로 요청 ({},{}) → ({},{})", fromLat, fromLng, toLat, toLng);
            Map<String, Object> body = buildBody(fromLat, fromLng, toLat, toLng, "출발지", "도착지");
            Map<?, ?> response = post(CAR_URL, body);
            int totalSeconds = extractTotalTime(response);
            int taxiFare = extractTaxiFare(response);
            int minutes = totalSeconds > 0 ? (int) Math.ceil(totalSeconds / 60.0) : -1;
            List<double[]> path = extractPath(response);
            log.info("[TMap] 자동차경로 완료 — {}분, {}원, 좌표{}개", minutes, taxiFare, path.size());
            return new CarRouteResult(minutes, taxiFare, path);
        } catch (Exception e) {
            log.warn("[TMap] 자동차 API 호출 실패: {}", e.getMessage());
            return CarRouteResult.failed();
        }
    }

    public int getWalkMinutes(double fromLat, double fromLng, double toLat, double toLng) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[TMap] API 키가 설정되지 않았습니다 (tmap.api.key)");
            return -1;
        }
        try {
            log.info("[TMap] 도보경로 요청 ({},{}) → ({},{})", fromLat, fromLng, toLat, toLng);
            Map<String, Object> body = buildBody(fromLat, fromLng, toLat, toLng, "출발지", "도착지");
            Map<?, ?> response = post(WALK_URL, body);
            int totalSeconds = extractTotalTime(response);
            int minutes = totalSeconds > 0 ? (int) Math.ceil(totalSeconds / 60.0) : -1;
            log.info("[TMap] 도보경로 완료 — {}분", minutes);
            return minutes;
        } catch (Exception e) {
            log.warn("[TMap] 도보 API 호출 실패: {}", e.getMessage());
            return -1;
        }
    }

    public int[] getCarRouteInfo(double fromLat, double fromLng, double toLat, double toLng) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[TMap] API 키가 설정되지 않았습니다 (tmap.api.key)");
            return new int[]{-1, -1};
        }
        try {
            log.info("[TMap] 자동차경로 요청 ({},{}) → ({},{})", fromLat, fromLng, toLat, toLng);
            Map<String, Object> body = buildBody(fromLat, fromLng, toLat, toLng, "출발지", "도착지");
            Map<?, ?> response = post(CAR_URL, body);
            int totalSeconds = extractTotalTime(response);
            int taxiFare = extractTaxiFare(response);
            int minutes = totalSeconds > 0 ? (int) Math.ceil(totalSeconds / 60.0) : -1;
            log.info("[TMap] 자동차경로 완료 — {}분, {}원", minutes, taxiFare);
            return new int[]{minutes, taxiFare};
        } catch (Exception e) {
            log.warn("[TMap] 자동차 API 호출 실패: {}", e.getMessage());
            return new int[]{-1, -1};
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

    @SuppressWarnings("unchecked")
    private List<double[]> extractPath(Map<?, ?> response) {
        if (response == null) return Collections.emptyList();
        List<?> features = (List<?>) response.get("features");
        if (features == null) return Collections.emptyList();

        List<double[]> path = new ArrayList<>();
        for (Object f : features) {
            Map<?, ?> feature = (Map<?, ?>) f;
            Map<?, ?> geometry = (Map<?, ?>) feature.get("geometry");
            if (geometry == null) continue;
            String type = (String) geometry.get("type");
            if ("LineString".equals(type)) {
                List<?> coords = (List<?>) geometry.get("coordinates");
                if (coords == null) continue;
                for (Object c : coords) {
                    List<?> coord = (List<?>) c;
                    if (coord.size() >= 2) {
                        double lng = ((Number) coord.get(0)).doubleValue();
                        double lat = ((Number) coord.get(1)).doubleValue();
                        path.add(new double[]{lat, lng});
                    }
                }
            }
        }
        return path;
    }
}
