package com.ssafy.layover.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.layover.place.Place;
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

@Component
public class AiCourseClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.course.enabled:true}")
    private boolean enabled;

    @Value("${ai.course.api-key:}")
    private String apiKey;

    @Value("${ai.course.base-url:}")
    private String baseUrl;

    @Value("${ai.course.model:gpt-4o-mini}")
    private String model;

    public AiCourseClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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

        try {
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

            String response = restTemplate.postForObject(baseUrl, new HttpEntity<>(body, headers), String.class);
            return parsePlans(response, courseCount, placeCount, candidates);
        } catch (Exception e) {
            return List.of();
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

    private String stationCoordinateHint(String departureStation) {
        String normalized = departureStation == null ? "" : departureStation.trim().toUpperCase();
        if (normalized.contains("SEO") || normalized.contains("SEODDAEJEON") || normalized.contains("서대전")) {
            return "Seo-Daejeon Station lat=36.3226, lng=127.4039";
        }
        return "Daejeon Station lat=36.3325, lng=127.4348";
    }

    public record AiCoursePlan(String title, List<String> placeIds) {
    }
}
