package com.ssafy.layover.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 기상청 단기예보(초단기실황) API로 현재 날씨를 조회한다.
 *
 * <p>코스 추천 프롬프트에는 "비/눈이거나 매우 덥거나 추우면 실내를 우선한다"는 규칙이
 * 이미 있었지만, 프론트엔드가 weatherCondition을 채우지 않아 항상 null이 전달됐다.
 * 이제 백엔드가 출발역 좌표 기준으로 값을 채운다.
 *
 * <p>날씨 조회가 실패해도 코스 추천은 그대로 진행한다. 날씨는 추천 품질을 높이는
 * 보조 정보일 뿐이므로, 외부 API 장애가 핵심 기능을 막아서는 안 된다.
 */
@Slf4j
@Service
public class WeatherService {

    /** 하늘 상태를 알 수 없을 때 쓰는 값. 프롬프트에서 특별한 규칙을 적용하지 않는다. */
    public static final String UNKNOWN = "UNKNOWN";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");

    /** 초단기실황은 매시 정시 발표분이 40분쯤 뒤에 올라온다. */
    private static final int PUBLISH_DELAY_MINUTES = 40;

    /** 날씨는 천천히 변한다. 같은 격자는 이 시간 동안 재사용한다. */
    private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000L;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    @Value("${weather.enabled:true}")
    private boolean enabled;

    @Value("${weather.api.key:}")
    private String serviceKey;

    @Value("${weather.api.base-url:https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}")
    private String baseUrl;

    public WeatherService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private record CachedWeather(String condition, long expiresAt) {
    }

    /**
     * 해당 좌표의 현재 날씨 상태를 반환한다.
     * 실패하면 {@link #UNKNOWN} 을 반환하며 예외를 던지지 않는다.
     */
    public String getCurrentCondition(double latitude, double longitude) {
        if (!enabled || serviceKey == null || serviceKey.isBlank()) {
            return UNKNOWN;
        }

        KmaGridConverter.Grid grid = KmaGridConverter.toGrid(latitude, longitude);
        String cacheKey = grid.nx() + ":" + grid.ny();

        CachedWeather cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
            return cached.condition();
        }

        String condition = fetchCondition(grid);
        cache.put(cacheKey, new CachedWeather(condition, System.currentTimeMillis() + CACHE_TTL_MILLIS));
        return condition;
    }

    private String fetchCondition(KmaGridConverter.Grid grid) {
        try {
            LocalDateTime baseAt = LocalDateTime.now().minusMinutes(PUBLISH_DELAY_MINUTES).withMinute(0);
            String url = baseUrl + "/getUltraSrtNcst"
                    + "?serviceKey=" + URLEncoder.encode(serviceKey, StandardCharsets.UTF_8)
                    + "&dataType=JSON&numOfRows=100&pageNo=1"
                    + "&base_date=" + baseAt.format(DATE_FMT)
                    + "&base_time=" + baseAt.format(TIME_FMT)
                    + "&nx=" + grid.nx()
                    + "&ny=" + grid.ny();

            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isBlank()) return UNKNOWN;

            // 키가 잘못되었거나 미승인이면 XML 에러 문서가 돌아온다.
            if (body.trim().startsWith("<")) {
                log.warn("[Weather] 기상청 API가 JSON이 아닌 응답을 반환했습니다. 서비스키 승인 상태를 확인하세요.");
                return UNKNOWN;
            }

            JsonNode items = objectMapper.readTree(body)
                    .path("response").path("body").path("items").path("item");
            if (!items.isArray() || items.isEmpty()) {
                log.warn("[Weather] 기상청 응답에 관측값이 없습니다. grid=({}, {})", grid.nx(), grid.ny());
                return UNKNOWN;
            }

            Integer precipitationType = null;
            Double temperature = null;
            for (JsonNode item : items) {
                String category = item.path("category").asText("");
                String value = item.path("obsrValue").asText("");
                if (value.isBlank()) continue;

                if ("PTY".equals(category)) {
                    precipitationType = safeInt(value);
                } else if ("T1H".equals(category)) {
                    temperature = safeDouble(value);
                }
            }

            String condition = toCondition(precipitationType, temperature);
            log.info("[Weather] grid=({}, {}) PTY={} T1H={} -> {}",
                    grid.nx(), grid.ny(), precipitationType, temperature, condition);
            return condition;
        } catch (Exception e) {
            log.warn("[Weather] 날씨 조회에 실패해 날씨 조건 없이 코스를 추천합니다: {}", e.getMessage());
            return UNKNOWN;
        }
    }

    /**
     * PTY(강수형태)와 T1H(기온)를 코스 추천 프롬프트가 이해하는 값으로 바꾼다.
     * PTY: 0 없음, 1 비, 2 비/눈, 3 눈, 4 소나기, 5 빗방울, 6 빗방울눈날림, 7 눈날림
     */
    static String toCondition(Integer precipitationType, Double temperature) {
        if (precipitationType != null) {
            switch (precipitationType) {
                case 1, 4, 5 -> {
                    return "RAIN";
                }
                case 2, 6 -> {
                    return "SLEET";
                }
                case 3, 7 -> {
                    return "SNOW";
                }
                default -> {
                    // 강수 없음. 기온으로 판단을 이어간다.
                }
            }
        }

        if (temperature != null) {
            if (temperature >= 33) return "HOT";
            if (temperature <= -5) return "COLD";
        }

        if (precipitationType == null && temperature == null) return UNKNOWN;
        return "CLEAR";
    }

    private static Integer safeInt(String value) {
        try {
            return (int) Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double safeDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
