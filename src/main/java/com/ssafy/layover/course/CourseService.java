package com.ssafy.layover.course;

import com.ssafy.layover.bus.BusService;
import com.ssafy.layover.course.dto.*;
import com.ssafy.layover.place.Place;
import com.ssafy.layover.place.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final PlaceRepository placeRepository;
    private final BusService busService;

    // 코스 3개 생성 — DB에 저장하지 않고 DTO만 반환
    // 확정하기 버튼 누를 때 저장 (추후 auth 연동 후 구현)
    public List<CourseResponse> generateCourses(CourseGenerateRequest req) {
        List<Place> candidates = selectCandidates(req.getThemeTags());
        if (candidates.size() < 2) {
            candidates = placeRepository.findByIsActiveTrueAndLatitudeIsNotNullAndLongitudeIsNotNull();
        }

        int placeCount = placeCountFor(req.getDurationMinutes());
        Random rng = new Random();

        List<CourseResponse> results = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            List<Place> picked = pickRandom(candidates, placeCount, rng);
            results.add(buildResponse(i, picked, req.getTravelMode()));
        }
        return results;
    }

    private List<Place> selectCandidates(List<String> themeTags) {
        if (themeTags == null || themeTags.isEmpty()) {
            return placeRepository.findByIsActiveTrueAndLatitudeIsNotNullAndLongitudeIsNotNull();
        }
        return placeRepository.findByCategoryInAndIsActiveTrue(themeTags);
    }

    private int placeCountFor(int durationMinutes) {
        if (durationMinutes <= 60)  return 2;
        if (durationMinutes <= 120) return 3;
        return 4;
    }

    private List<Place> pickRandom(List<Place> pool, int count, Random rng) {
        List<Place> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, rng);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private CourseResponse buildResponse(int index, List<Place> places, String travelMode) {
        String[] titles = {"코스 A", "코스 B", "코스 C"};

        List<CourseStopResponse> stops = new ArrayList<>();
        int totalMinutes = 0;
        int totalFare = 0;

        for (int i = 0; i < places.size(); i++) {
            Place cur = places.get(i);
            String stayTime = stayTimeFor(cur.getCategory());
            totalMinutes += parseMin(stayTime);

            TransportInfoResponse transport = null;
            if (i < places.size() - 1) {
                transport = calcTransport(cur, places.get(i + 1));
                boolean isWalk = "WALK".equals(travelMode);
                totalMinutes += parseMin(isWalk ? transport.getWalkTime() : transport.getTaxiTime());
                if (!isWalk) totalFare += transport.getTaxiFare();
            }
            stops.add(new CourseStopResponse(cur, stayTime, transport, travelMode));
        }

        String subTitle = places.stream().map(Place::getName).collect(Collectors.joining(" → "));
        return new CourseResponse(
                UUID.randomUUID().toString(),
                titles[index],
                subTitle,
                formatMin(totalMinutes),
                "약 " + String.format("%,d", totalFare) + "원",
                stops
        );
    }

    // 두 장소 사이 이동시간·요금 계산
    // 도보/택시 — Haversine 직선거리 기반, 버스 — 실제 정류소 경유 추정
    private TransportInfoResponse calcTransport(Place from, Place to) {
        double fLat = from.getLatitude().doubleValue(), fLng = from.getLongitude().doubleValue();
        double tLat = to.getLatitude().doubleValue(),   tLng = to.getLongitude().doubleValue();
        double dist = haversine(fLat, fLng, tLat, tLng);

        int walkMin = (int) Math.round(dist / 4.0 * 60);
        int taxiMin = Math.max(3, (int) Math.round(dist / 30.0 * 60));
        int busMin  = busService.estimateBusMinutes(fLat, fLng, tLat, tLng);
        int fare    = 3800 + (int) (dist * 800);

        return new TransportInfoResponse(walkMin + "분", busMin + "분", taxiMin + "분", fare);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String stayTimeFor(String category) {
        return switch (category) {
            case "FOOD"    -> "60분";
            case "CAFE"    -> "30분";
            case "NATURE"  -> "60분";
            case "CULTURE" -> "45분";
            default        -> "45분";
        };
    }

    private int parseMin(String timeStr) {
        try {
            return Integer.parseInt(timeStr.replace("분", "").trim());
        } catch (Exception e) {
            return 30;
        }
    }

    private String formatMin(int minutes) {
        int h = minutes / 60, m = minutes % 60;
        return h > 0 ? "약 " + h + "시간 " + m + "분" : "약 " + m + "분";
    }
}
