package com.ssafy.layover.place;

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

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoLocalApiClient {

    private static final String KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final double MAX_MATCH_DISTANCE_METERS = 700.0;

    private final RestTemplate restTemplate;

    @Value("${kakao.rest-api-key:${kakao.api.key:}}")
    private String restApiKey;

    public Optional<KakaoPlaceMatch> findBestMatch(String name, Double latitude, Double longitude) {
        if (!hasApiKey() || name == null || name.isBlank() || latitude == null || longitude == null) {
            return Optional.empty();
        }

        try {
            String uri = UriComponentsBuilder.fromUriString(KEYWORD_URL)
                    .queryParam("query", name)
                    .queryParam("x", longitude)
                    .queryParam("y", latitude)
                    .queryParam("radius", 1000)
                    .queryParam("size", 5)
                    .build()
                    .encode()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + restApiKey);
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Object documents = response.getBody() != null ? response.getBody().get("documents") : null;
            if (!(documents instanceof List<?> list) || list.isEmpty()) {
                return Optional.empty();
            }

            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(doc -> toMatch(doc, latitude, longitude))
                    .filter(match -> match.distanceMeters() <= MAX_MATCH_DISTANCE_METERS)
                    .min((a, b) -> Double.compare(a.distanceMeters(), b.distanceMeters()));
        } catch (Exception e) {
            log.debug("[KakaoLocal] place match failed for {}: {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    private KakaoPlaceMatch toMatch(Map<?, ?> doc, Double latitude, Double longitude) {
        double kakaoLat = parseDouble(doc.get("y"));
        double kakaoLng = parseDouble(doc.get("x"));
        return new KakaoPlaceMatch(
                str(doc, "id"),
                str(doc, "place_url"),
                str(doc, "phone"),
                str(doc, "road_address_name"),
                distanceMeters(latitude, longitude, kakaoLat, kakaoLng)
        );
    }

    private boolean hasApiKey() {
        return restApiKey != null && !restApiKey.isBlank();
    }

    private String str(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    private double parseDouble(Object value) {
        if (value == null) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    public record KakaoPlaceMatch(
            String id,
            String placeUrl,
            String phone,
            String roadAddress,
            double distanceMeters
    ) {
    }
}
