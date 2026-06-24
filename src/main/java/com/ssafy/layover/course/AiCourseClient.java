package com.ssafy.layover.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.layover.place.Place;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiCourseClient {

    private static final int MAX_ATTEMPTS = 3;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean blocked = new AtomicBoolean(false);

    @Value("${ai.course.enabled:true}")
    private boolean enabled;

    @Value("${ai.course.api-key:}")
    private String apiKey;

    @Value("${ai.course.base-url:}")
    private String baseUrl;

    @Value("${ai.course.model:gpt-4o-mini}")
    private String model;

    public AiCourseClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isBlocked() {
        return blocked.get();
    }

    public List<AiCoursePlan> recommendCourses(
            CourseGenerateRequest req,
            List<Place> candidates,
            int placeCount,
            int courseCount,
            List<String> lockedPlaceIds
    ) {
        if (!enabled || isBlank(apiKey) || isBlank(baseUrl) || candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        if (blocked.get()) {
            log.warn("[AI Course] 실패했습니다 - 이전 코스 추천 API 실패로 추가 호출을 차단합니다.");
            return List.of();
        }

        String prompt = buildPrompt(req, candidates, placeCount, courseCount, lockedPlaceIds);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("messages", List.of(
                Map.of("role", "system", "content",
                        "You are a strict Daejeon rail layover route optimizer. "
                                + "You must obey time, locked-place, and candidate-id constraints. "
                                + "Return only valid JSON. Do not include markdown."),
                Map.of("role", "user", "content", prompt)
        ));
        body.put("response_format", Map.of("type", "json_object"));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String response = restTemplate.postForObject(baseUrl, new HttpEntity<>(body, headers), String.class);
                return parsePlans(response, courseCount, placeCount, candidates);
            } catch (Exception e) {
                log.warn("[AI Course] 코스 추천 API 호출 실패 {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }

        blockFurtherCalls();
        return List.of();
    }

    private void blockFurtherCalls() {
        if (blocked.compareAndSet(false, true)) {
            log.error("[AI Course] 실패했습니다 - 코스 추천 API {}회 시도 실패로 추가 호출을 차단합니다.", MAX_ATTEMPTS);
        }
    }

    private String buildPrompt(
            CourseGenerateRequest req,
            List<Place> candidates,
            int placeCount,
            int courseCount,
            List<String> lockedPlaceIds
    ) {
        StringBuilder sb = new StringBuilder();
        double maxRadiusKm = radiusKmForPrompt(req.getDurationMinutes(), "WALK".equalsIgnoreCase(req.getTravelMode()));
        double[] stationCoordinate = stationCoordinate(req.getDepartureStation());
        sb.append("Task: recommend Daejeon layover travel courses.\n");
        sb.append("Return JSON only. No markdown, no comments, no extra text.\n");
        sb.append("Required JSON schema: {\"courses\":[{\"title\":\"Korean course title\",\"placeIds\":[\"id1\",\"id2\"]}]}\n");
        sb.append("\n");
        sb.append("Hard constraints:\n");
        sb.append("1. Choose ONLY from the candidate place ids below. Never invent ids or place names.\n");
        sb.append("2. Each course must contain exactly ").append(placeCount).append(" placeIds unless there are not enough candidates.\n");
        sb.append("3. Each course must fit within availableDurationMinutes, including estimated stay time and travel time.\n");
        sb.append("4. The time budget includes going from the departure station to the first place AND returning from the last place to the departure station.\n");
        sb.append("5. Prefer currently open places. Avoid closed places unless there are no good alternatives.\n");
        sb.append("6. If locked place ids are provided, every returned course must include all locked ids.\n");
        sb.append("7. Keep route order geographically reasonable for a short rail layover and finish near enough to return safely.\n");
        sb.append("8. Candidate places are pre-filtered by station radius. Prefer closer candidates first. Current radius limit: ")
                .append(String.format("%.1f", maxRadiusKm)).append("km from the departure station.\n");
        sb.append("\n");
        sb.append("Stay-time estimates to use:\n");
        sb.append("- FOOD: 60 minutes\n");
        sb.append("- CAFE: 30 minutes\n");
        sb.append("- NATURE: 60 minutes\n");
        sb.append("- CULTURE: 45 minutes\n");
        sb.append("- TOUR or other: 45 minutes\n");
        sb.append("\n");
        sb.append("Course style targets:\n");
        if (courseCount >= 3) {
            sb.append("- Course 1: quick course, about 50-60% of available time.\n");
            sb.append("- Course 2: relaxed course, about 65-75% of available time.\n");
            sb.append("- Course 3: tight but safe course, about 80-90% of available time.\n");
        } else {
            sb.append("- Make the course balanced and safe, about 70-85% of available time.\n");
        }
        sb.append("\n");
        sb.append("Request context:\n");
        sb.append("Number of courses: ").append(courseCount).append("\n");
        sb.append("Departure station: ").append(req.getDepartureStation()).append("\n");
        sb.append("Departure station coordinates: ").append(stationCoordinateHint(req.getDepartureStation())).append("\n");
        sb.append("Available duration minutes: ").append(req.getDurationMinutes()).append("\n");
        sb.append("Travel mode: ").append(req.getTravelMode()).append("\n");
        sb.append("Weather: ").append(req.getWeatherCondition()).append("\n");
        sb.append("Preference tags: ").append(req.getThemeTags()).append("\n");
        sb.append("Weather rule: if weather is rainy, snowy, or very hot/cold, prefer indoor categories such as FOOD, CAFE, CULTURE.\n");
        if (lockedPlaceIds != null && !lockedPlaceIds.isEmpty()) {
            sb.append("Must include locked place ids: ").append(lockedPlaceIds).append("\n");
        }
        sb.append("\n");
        sb.append("Candidate places:\n");

        candidates.stream()
                .limit(90)
                .forEach(place -> sb.append("- id=").append(place.getId())
                        .append(", name=").append(place.getName())
                        .append(", category=").append(place.getCategory())
                        .append(", lat=").append(place.getLatitude())
                        .append(", lng=").append(place.getLongitude())
                        .append(", stationDistanceKm=")
                        .append(String.format("%.2f", distanceKm(stationCoordinate[0], stationCoordinate[1], place)))
                        .append(", address=").append(nullToEmpty(place.getAddress()))
                        .append(", open=").append(place.isCurrentlyOpen())
                        .append("\n"));

        sb.append("Make routes realistic for a short layover. Balance food, cafe, nature, culture, and tourism when possible.");
        return sb.toString();
    }

    private List<AiCoursePlan> parsePlans(String response, int courseCount, int placeCount, List<Place> candidates) throws Exception {
        if (isBlank(response)) return List.of();

        Set<String> candidateIds = new HashSet<>();
        for (Place candidate : candidates) {
            candidateIds.add(candidate.getId());
        }

        JsonNode root = objectMapper.readTree(response);
        String content = root.path("choices").path(0).path("message").path("content").asText(null);
        JsonNode contentRoot = isBlank(content) ? root : objectMapper.readTree(content);
        JsonNode coursesNode = contentRoot.path("courses");

        List<AiCoursePlan> plans = new ArrayList<>();
        if (!coursesNode.isArray()) return plans;

        for (JsonNode courseNode : coursesNode) {
            if (plans.size() >= courseCount) break;
            JsonNode idsNode = courseNode.path("placeIds");
            if (!idsNode.isArray()) continue;

            List<String> ids = new ArrayList<>();
            for (JsonNode idNode : idsNode) {
                String id = idNode.asText();
                if (candidateIds.contains(id) && !ids.contains(id)) {
                    ids.add(id);
                }
                if (ids.size() >= placeCount) break;
            }

            if (!ids.isEmpty()) {
                String title = courseNode.path("title").asText("AI recommended course");
                plans.add(new AiCoursePlan(title, ids));
            }
        }
        return plans;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private double radiusKmForPrompt(int durationMinutes, boolean walkOnly) {
        if (walkOnly) {
            if (durationMinutes <= 180) return 2.0;
            if (durationMinutes <= 300) return 4.0;
            return 6.0;
        }

        if (durationMinutes <= 180) return 3.0;
        if (durationMinutes <= 300) return 7.0;
        return 12.0;
    }

    private double[] stationCoordinate(String departureStation) {
        String normalized = departureStation == null ? "" : departureStation.trim().toUpperCase();
        if (normalized.contains("SINTANJIN") || normalized.contains("신탄진")) {
            return new double[]{36.4518, 127.4297};
        }
        if (normalized.contains("SEO") || normalized.contains("SEODAEJEON") || normalized.contains("SEODDAEJEON")
                || normalized.contains("서대전")) {
            return new double[]{36.3226, 127.4039};
        }
        return new double[]{36.3325, 127.4348};
    }

    private double distanceKm(double stationLat, double stationLng, Place place) {
        if (place.getLatitude() == null || place.getLongitude() == null) {
            return 999.0;
        }
        double lat1 = Math.toRadians(stationLat);
        double lat2 = Math.toRadians(place.getLatitude().doubleValue());
        double dLat = Math.toRadians(place.getLatitude().doubleValue() - stationLat);
        double dLng = Math.toRadians(place.getLongitude().doubleValue() - stationLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String stationCoordinateHint(String departureStation) {
        String normalized = departureStation == null ? "" : departureStation.trim().toUpperCase();
        if (normalized.contains("SINTANJIN") || normalized.contains("신탄진")) {
            return "Sintanjin Station lat=36.4518, lng=127.4297";
        }
        if (normalized.contains("SEO") || normalized.contains("SEODAEJEON") || normalized.contains("SEODDAEJEON")
                || normalized.contains("서대전")) {
            return "Seo-Daejeon Station lat=36.3226, lng=127.4039";
        }
        return "Daejeon Station lat=36.3325, lng=127.4348";
    }

    // 3-course generation: 2 standard + 1 extended (durationMinutes+60)
    public List<AiCoursePlan> recommendCourses(
            CourseGenerateRequest req,
            List<Place> standardCandidates,
            List<Place> extendedCandidates,
            int placeCount,
            int extendedPlaceCount,
            int courseCount,
            List<String> lockedPlaceIds
    ) {
        if (!enabled || isBlank(apiKey) || isBlank(baseUrl)) {
            return List.of();
        }
        if (blocked.get()) {
            log.warn("[AI Course] 실패했습니다 - 이전 코스 추천 API 실패로 추가 호출을 차단합니다.");
            return List.of();
        }
        List<Place> allCandidates = (extendedCandidates != null && !extendedCandidates.isEmpty())
                ? extendedCandidates : standardCandidates;
        if (allCandidates == null || allCandidates.isEmpty()) return List.of();

        String prompt = buildPromptWithExtended(req, standardCandidates, extendedCandidates,
                placeCount, extendedPlaceCount, courseCount, lockedPlaceIds);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("messages", List.of(
                Map.of("role", "system", "content",
                        "You are a strict Daejeon rail layover route optimizer. "
                                + "You must obey time, category, locked-place, and candidate-id constraints. "
                                + "Return only valid JSON. Do not include markdown."),
                Map.of("role", "user", "content", prompt)
        ));
        body.put("response_format", Map.of("type", "json_object"));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String response = restTemplate.postForObject(baseUrl, new HttpEntity<>(body, headers), String.class);
                return parsePlansWithExtended(response, courseCount, placeCount, extendedPlaceCount,
                        standardCandidates, extendedCandidates);
            } catch (Exception e) {
                log.warn("[AI Course] 코스 추천 API 호출 실패 {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }
        blockFurtherCalls();
        return List.of();
    }

    private String buildPromptWithExtended(
            CourseGenerateRequest req,
            List<Place> standardCandidates,
            List<Place> extendedCandidates,
            int placeCount,
            int extendedPlaceCount,
            int courseCount,
            List<String> lockedPlaceIds
    ) {
        int durationMinutes = req.getDurationMinutes();
        int extendedDuration = durationMinutes + 60;
        boolean walkOnly = "WALK".equalsIgnoreCase(req.getTravelMode());
        double standardRadius = radiusKmForPrompt(durationMinutes, walkOnly);
        double extendedRadius = radiusKmForPrompt(extendedDuration, walkOnly);
        double[] stationCoord = stationCoordinate(req.getDepartureStation());

        Set<String> standardIds = standardCandidates.stream().map(Place::getId).collect(Collectors.toSet());
        List<Place> promptCandidates = (extendedCandidates != null && !extendedCandidates.isEmpty())
                ? extendedCandidates : standardCandidates;

        StringBuilder sb = new StringBuilder();
        sb.append("Task: recommend Daejeon layover travel courses.\n");
        sb.append("Return JSON only. No markdown, no comments, no extra text.\n");
        sb.append("Required JSON schema: {\"courses\":[{\"title\":\"Korean course title\",\"placeIds\":[\"id1\",\"id2\"]}]}\n\n");

        sb.append("Hard constraints:\n");
        sb.append("1. Choose ONLY from the candidate place ids listed below. Never invent ids or names.\n");
        sb.append("2. Courses 1 and 2: exactly ").append(placeCount).append(" placeIds each.\n");
        sb.append("3. Course 3 (extended): exactly ").append(extendedPlaceCount).append(" placeIds.\n");
        sb.append("4. Each course must fit within its time budget including station→first and last→station travel.\n");
        sb.append("5. Prefer currently open places.\n");
        sb.append("6. Courses 1 and 2 must be meaningfully different — share fewer than half their places.\n");
        sb.append("7. Courses 1 and 2 category rules:\n");
        sb.append("   - No two consecutive places may share the same category.\n");
        sb.append("   - At most 1 FOOD place per course.\n");
        sb.append("   - At most 2 CAFE places per course.\n");
        sb.append("8. Courses 1 and 2: prefer places within ").append(String.format("%.1f", standardRadius)).append("km of the station (extendedOnly=true places are off-limits).\n");
        sb.append("9. Course 3 (extended): may use all candidates including extendedOnly=true places up to ")
                .append(String.format("%.1f", extendedRadius)).append("km. Visit more places or farther destinations than courses 1 and 2.\n");
        if (lockedPlaceIds != null && !lockedPlaceIds.isEmpty()) {
            sb.append("10. Must include locked place ids in every course: ").append(lockedPlaceIds).append("\n");
        }
        sb.append("\n");

        sb.append("Stay-time estimates:\n");
        sb.append("- FOOD: 60 min  - CAFE: 30 min  - NATURE: 60 min  - CULTURE: 45 min  - TOUR/other: 45 min\n\n");

        sb.append("Time budgets:\n");
        sb.append("- Course 1 (quick): ").append(durationMinutes).append(" min, ~50-60% utilization, ").append(placeCount).append(" places.\n");
        sb.append("- Course 2 (relaxed): ").append(durationMinutes).append(" min, ~65-75% utilization, ").append(placeCount).append(" places.\n");
        sb.append("- Course 3 (extended): ").append(extendedDuration).append(" min, ~80-90% utilization, ").append(extendedPlaceCount).append(" places.\n\n");

        sb.append("Request context:\n");
        sb.append("Departure station: ").append(req.getDepartureStation()).append(" — ").append(stationCoordinateHint(req.getDepartureStation())).append("\n");
        sb.append("Travel mode: ").append(req.getTravelMode()).append("\n");
        sb.append("Weather: ").append(req.getWeatherCondition())
                .append(" (if rainy/snowy/very hot/cold, prefer indoor: FOOD, CAFE, CULTURE)\n");
        sb.append("Preference tags: ").append(req.getThemeTags()).append("\n\n");

        sb.append("Candidate places:\n");
        promptCandidates.stream()
                .limit(90)
                .forEach(place -> {
                    boolean extendedOnly = !standardIds.contains(place.getId());
                    sb.append("- id=").append(place.getId())
                            .append(", name=").append(place.getName())
                            .append(", category=").append(place.getCategory())
                            .append(", stationDistanceKm=")
                            .append(String.format("%.2f", distanceKm(stationCoord[0], stationCoord[1], place)))
                            .append(", open=").append(place.isCurrentlyOpen());
                    if (extendedOnly) sb.append(", extendedOnly=true");
                    sb.append("\n");
                });

        sb.append("\nBalance food, culture, and tourism when possible. Make routes geographically realistic for a short layover.");
        return sb.toString();
    }

    private List<AiCoursePlan> parsePlansWithExtended(String response, int courseCount, int placeCount,
                                                       int extendedPlaceCount,
                                                       List<Place> standardCandidates,
                                                       List<Place> extendedCandidates) throws Exception {
        if (isBlank(response)) return List.of();

        Set<String> standardIds = new HashSet<>();
        for (Place p : standardCandidates) standardIds.add(p.getId());
        Set<String> extendedIds = new HashSet<>();
        for (Place p : extendedCandidates) extendedIds.add(p.getId());
        if (extendedIds.isEmpty()) extendedIds = standardIds;

        JsonNode root = objectMapper.readTree(response);
        String content = root.path("choices").path(0).path("message").path("content").asText(null);
        JsonNode contentRoot = isBlank(content) ? root : objectMapper.readTree(content);
        JsonNode coursesNode = contentRoot.path("courses");

        List<AiCoursePlan> plans = new ArrayList<>();
        if (!coursesNode.isArray()) return plans;

        int courseIdx = 0;
        for (JsonNode courseNode : coursesNode) {
            if (plans.size() >= courseCount) break;
            JsonNode idsNode = courseNode.path("placeIds");
            if (!idsNode.isArray()) { courseIdx++; continue; }

            boolean isExtended = courseIdx == 2;
            Set<String> validIds = isExtended ? extendedIds : standardIds;
            int limit = isExtended ? extendedPlaceCount : placeCount;

            List<String> ids = new ArrayList<>();
            for (JsonNode idNode : idsNode) {
                String id = idNode.asText();
                if (validIds.contains(id) && !ids.contains(id)) ids.add(id);
                if (ids.size() >= limit) break;
            }

            if (!ids.isEmpty()) {
                plans.add(new AiCoursePlan(courseNode.path("title").asText("AI 추천 코스"), ids));
            }
            courseIdx++;
        }
        return plans;
    }

    public record AiCoursePlan(String title, List<String> placeIds) {
    }
}
