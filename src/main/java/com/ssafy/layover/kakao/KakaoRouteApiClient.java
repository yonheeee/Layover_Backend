package com.ssafy.layover.kakao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoRouteApiClient {

    private static final String WALK_URL = "https://dapi.kakao.com/v2/routing/walk";
    private static final String PUBLIC_TRAFFIC_URL = "https://dapi.kakao.com/v2/routing/publictraffic";
    private static final String CAR_URL = "https://apis-navi.kakaomobility.com/v1/directions";

    private final RestTemplate restTemplate;

    @Value("${kakao.rest-api-key:${kakao.api.key:}}")
    private String restApiKey;

    public record WalkRouteResult(int minutes, List<double[]> path) {
        public static WalkRouteResult failed() {
            return new WalkRouteResult(-1, Collections.emptyList());
        }
    }

    public record PublicTransitRouteResult(int minutes) {
        public static PublicTransitRouteResult failed() {
            return new PublicTransitRouteResult(-1);
        }
    }

    public record CarRouteResult(int minutes, int taxiFare, List<double[]> path) {
        public static CarRouteResult failed() {
            return new CarRouteResult(-1, -1, Collections.emptyList());
        }
    }

    public WalkRouteResult getWalkRouteResult(double fromLat, double fromLng, double toLat, double toLng) {
        if (!hasKey()) {
            log.warn("[KakaoRoute] REST API key is not configured. Set kakao.rest-api-key.");
            return WalkRouteResult.failed();
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(WALK_URL)
                    .queryParam("start_x", fromLng)
                    .queryParam("start_y", fromLat)
                    .queryParam("end_x", toLng)
                    .queryParam("end_y", toLat)
                    .queryParam("input_coord", "WGS84")
                    .queryParam("output_coord", "WGS84")
                    .queryParam("route_mode", "BROAD_FIRST")
                    .build()
                    .toUri();

            Map<?, ?> response = get(uri);
            Map<?, ?> route = asMap(response.get("route"));
            Map<?, ?> properties = route != null ? asMap(route.get("properties")) : null;
            int totalSeconds = intValue(properties, "totalTime", -1);
            int minutes = secondsToMinutes(totalSeconds);
            List<double[]> path = extractWalkPath(route);
            log.info("[KakaoRoute] walk route completed - {} minutes, {} points", minutes, path.size());
            return new WalkRouteResult(minutes, path);
        } catch (Exception e) {
            log.warn("[KakaoRoute] walk route failed: {}", e.getMessage());
            return WalkRouteResult.failed();
        }
    }

    public PublicTransitRouteResult getPublicTransitRouteResult(double fromLat, double fromLng, double toLat, double toLng) {
        if (!hasKey()) {
            log.warn("[KakaoRoute] REST API key is not configured. Set kakao.rest-api-key.");
            return PublicTransitRouteResult.failed();
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(PUBLIC_TRAFFIC_URL)
                    .queryParam("start_x", fromLng)
                    .queryParam("start_y", fromLat)
                    .queryParam("end_x", toLng)
                    .queryParam("end_y", toLat)
                    .queryParam("input_coord", "WGS84")
                    .queryParam("output_coord", "WGS84")
                    .build()
                    .toUri();

            Map<?, ?> response = get(uri);
            List<?> routes = asList(response.get("routes"));
            if (routes == null || routes.isEmpty()) return PublicTransitRouteResult.failed();
            Map<?, ?> firstRoute = asMap(routes.get(0));
            Map<?, ?> properties = firstRoute != null ? asMap(firstRoute.get("properties")) : null;
            int minutes = secondsToMinutes(intValue(properties, "totalTime", -1));
            log.info("[KakaoRoute] public transit route completed - {} minutes", minutes);
            return new PublicTransitRouteResult(minutes);
        } catch (Exception e) {
            log.warn("[KakaoRoute] public transit route failed: {}", e.getMessage());
            return PublicTransitRouteResult.failed();
        }
    }

    public CarRouteResult getCarRouteResult(double fromLat, double fromLng, double toLat, double toLng) {
        if (!hasKey()) {
            log.warn("[KakaoRoute] REST API key is not configured. Set kakao.rest-api-key.");
            return CarRouteResult.failed();
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(CAR_URL)
                    .queryParam("origin", fromLng + "," + fromLat)
                    .queryParam("destination", toLng + "," + toLat)
                    .queryParam("priority", "RECOMMEND")
                    .queryParam("summary", false)
                    .queryParam("car_fuel", "GASOLINE")
                    .queryParam("car_hipass", false)
                    .build()
                    .toUri();

            Map<?, ?> response = get(uri);
            List<?> routes = asList(response.get("routes"));
            if (routes == null || routes.isEmpty()) return CarRouteResult.failed();
            Map<?, ?> firstRoute = asMap(routes.get(0));
            if (firstRoute == null || intValue(firstRoute, "result_code", -1) != 0) {
                return CarRouteResult.failed();
            }

            Map<?, ?> summary = asMap(firstRoute.get("summary"));
            int minutes = secondsToMinutes(intValue(summary, "duration", -1));
            Map<?, ?> fare = summary != null ? asMap(summary.get("fare")) : null;
            int taxiFare = intValue(fare, "taxi", -1);
            List<double[]> path = extractCarPath(firstRoute);
            log.info("[KakaoRoute] car route completed - {} minutes, {} won, {} points", minutes, taxiFare, path.size());
            return new CarRouteResult(minutes, taxiFare, path);
        } catch (Exception e) {
            log.warn("[KakaoRoute] car route failed: {}", e.getMessage());
            return CarRouteResult.failed();
        }
    }

    private boolean hasKey() {
        return restApiKey != null && !restApiKey.isBlank();
    }

    private Map<?, ?> get(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + restApiKey);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, request, Map.class);
        return response.getBody();
    }

    private int secondsToMinutes(int seconds) {
        return seconds > 0 ? (int) Math.ceil(seconds / 60.0) : -1;
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<?, ?>) value : null;
    }

    private List<?> asList(Object value) {
        return value instanceof List<?> ? (List<?>) value : null;
    }

    private int intValue(Map<?, ?> map, String key, int defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    private List<double[]> extractWalkPath(Map<?, ?> route) {
        if (route == null) return Collections.emptyList();

        List<double[]> path = new ArrayList<>();
        List<?> legs = asList(route.get("legs"));
        if (legs == null) return path;

        for (Object legObj : legs) {
            Map<?, ?> leg = asMap(legObj);
            if (leg == null) continue;
            List<?> steps = asList(leg.get("steps"));
            if (steps == null) continue;

            for (Object stepObj : steps) {
                Map<?, ?> step = asMap(stepObj);
                Map<?, ?> stepPath = step != null ? asMap(step.get("path")) : null;
                List<?> points = stepPath != null ? asList(stepPath.get("points")) : null;
                if (points == null) continue;

                for (Object pointObj : points) {
                    List<?> point = asList(pointObj);
                    if (point != null && point.size() >= 2
                            && point.get(0) instanceof Number
                            && point.get(1) instanceof Number) {
                        double lng = ((Number) point.get(0)).doubleValue();
                        double lat = ((Number) point.get(1)).doubleValue();
                        path.add(new double[]{lat, lng});
                    }
                }
            }
        }
        return path;
    }

    private List<double[]> extractCarPath(Map<?, ?> route) {
        if (route == null) return Collections.emptyList();

        List<double[]> path = new ArrayList<>();
        List<?> sections = asList(route.get("sections"));
        if (sections == null) return path;

        for (Object sectionObj : sections) {
            Map<?, ?> section = asMap(sectionObj);
            if (section == null) continue;
            List<?> roads = asList(section.get("roads"));
            if (roads == null) continue;

            for (Object roadObj : roads) {
                Map<?, ?> road = asMap(roadObj);
                List<?> vertexes = road != null ? asList(road.get("vertexes")) : null;
                if (vertexes == null) continue;

                for (int i = 0; i + 1 < vertexes.size(); i += 2) {
                    Object lngObj = vertexes.get(i);
                    Object latObj = vertexes.get(i + 1);
                    if (lngObj instanceof Number && latObj instanceof Number) {
                        path.add(new double[]{
                                ((Number) latObj).doubleValue(),
                                ((Number) lngObj).doubleValue()
                        });
                    }
                }
            }
        }
        return path;
    }
}
