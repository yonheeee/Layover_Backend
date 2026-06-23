package com.ssafy.layover.course;

import com.ssafy.layover.bus.BusService;
import com.ssafy.layover.tmap.TMapApiClient;
import com.ssafy.layover.place.Place;
import com.ssafy.layover.place.PlaceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final PlaceMapper placeMapper;
    private final CourseMapper courseMapper;
    private final CoursePlaceMapper coursePlaceMapper;
    private final BusService busService;
    private final TMapApiClient tMapApiClient;
    private final AiCourseClient aiCourseClient;

    public CourseService(PlaceMapper placeMapper, CourseMapper courseMapper,
                         CoursePlaceMapper coursePlaceMapper, BusService busService,
                         TMapApiClient tMapApiClient,
                         AiCourseClient aiCourseClient) {
        this.placeMapper = placeMapper;
        this.courseMapper = courseMapper;
        this.coursePlaceMapper = coursePlaceMapper;
        this.busService = busService;
        this.tMapApiClient = tMapApiClient;
        this.aiCourseClient = aiCourseClient;
    }
    @Transactional
    public String saveCourse(String userId, SaveCourseRequest req) {
        String themeTagsJson = null;
        if (req.getThemeTags() != null && !req.getThemeTags().isEmpty()) {
            themeTagsJson = "[\"" + String.join("\",\"", req.getThemeTags()) + "\"]";
        }

        Course course = Course.create(userId, req.getDepartureStation(), req.getDurationMinutes(),
                req.getTravelMode(), req.getWeatherCondition(), themeTagsJson);
        courseMapper.insert(course);

        if (req.getPlaces() != null) {
            for (SaveCourseRequest.PlaceItem item : req.getPlaces()) {
                CoursePlace cp = CoursePlace.of(course.getId(), item.getPlaceId(),
                        item.getOrderIndex(), item.getTravelTimeMin());
                coursePlaceMapper.insert(cp);
            }
        }
        return course.getId();
    }

    public List<SavedCourseResponse> getMyCourses(String userId) {
        List<Course> courses = courseMapper.findByUserId(userId);
        return courses.stream().map(course -> {
            List<CoursePlace> coursePlaces = coursePlaceMapper.findByCourseId(course.getId());
            List<Place> places = coursePlaces.stream()
                    .map(cp -> placeMapper.findById(cp.getPlaceId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return SavedCourseResponse.from(course, places);
        }).collect(Collectors.toList());
    }

    @Transactional
    public void deleteCourse(String userId, String courseId) {
        Course course = courseMapper.findByIdAndUserId(courseId, userId);
        if (course == null) {
            throw new IllegalArgumentException("코스를 찾을 수 없거나 권한이 없습니다.");
        }
        coursePlaceMapper.deleteByCourseId(courseId);
        courseMapper.deleteById(courseId);
    }

    public List<CourseResponse> generateCourses(CourseGenerateRequest req) {
        List<Place> candidates = selectCandidates(req.getThemeTags());
        if (candidates.size() < 2) {
            candidates = placeMapper.findAllWithLocation();
        }

        int placeCount = placeCountFor(req.getDurationMinutes());
        Random rng = new Random();

        List<CourseResponse> results = new ArrayList<>();
        List<AiCourseClient.AiCoursePlan> aiPlans =
                aiCourseClient.recommendCourses(req, candidates, placeCount, 3, List.of());
        for (int i = 0; i < aiPlans.size() && results.size() < 3; i++) {
            List<Place> picked = placesByIds(aiPlans.get(i).placeIds(), candidates, placeCount);
            if (!picked.isEmpty()) {
                results.add(buildResponse(results.size(), aiPlans.get(i).title(), picked, req.getTravelMode()));
            }
        }

        while (results.size() < 3) {
            List<Place> picked = pickRandom(candidates, placeCount, rng);
            results.add(buildResponse(results.size(), picked, req.getTravelMode()));
        }
        return results;
    }

    public CourseResponse regenerateCourse(CourseRegenerateRequest req) {
        List<Place> candidates = selectCandidates(req.getThemeTags());
        if (candidates.size() < 2) {
            candidates = placeMapper.findAllWithLocation();
        }

        int placeCount = req.getCurrentPlaces() != null && !req.getCurrentPlaces().isEmpty()
                ? req.getCurrentPlaces().size()
                : placeCountFor(req.getDurationMinutes());

        List<String> lockedPlaceIds = req.getCurrentPlaces() == null
                ? List.of()
                : req.getCurrentPlaces().stream()
                .filter(CourseRegenerateRequest.CurrentPlace::isLocked)
                .map(CourseRegenerateRequest.CurrentPlace::getId)
                .filter(Objects::nonNull)
                .toList();

        List<AiCourseClient.AiCoursePlan> aiPlans =
                aiCourseClient.recommendCourses(req, candidates, placeCount, 1, lockedPlaceIds);
        List<Place> aiPicked = aiPlans.isEmpty()
                ? List.of()
                : placesByIds(aiPlans.get(0).placeIds(), candidates, placeCount);

        List<Place> merged = mergeLockedPlaces(req, aiPicked, candidates, placeCount);
        String title = aiPlans.isEmpty() ? "AI 재추천 코스" : aiPlans.get(0).title();
        return buildResponse(0, title, merged, req.getTravelMode());
    }

    private List<Place> selectCandidates(List<String> themeTags) {
        if (themeTags == null || themeTags.isEmpty()) {
            return placeMapper.findAllWithLocation();
        }
        return placeMapper.findByCategoryIn(themeTags);
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

    private List<Place> placesByIds(List<String> ids, List<Place> candidates, int limit) {
        Map<String, Place> byId = candidates.stream()
                .collect(Collectors.toMap(Place::getId, place -> place, (a, b) -> a, LinkedHashMap::new));
        List<Place> result = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                Place place = byId.get(id);
                if (place != null && result.stream().noneMatch(p -> p.getId().equals(place.getId()))) {
                    result.add(place);
                }
                if (result.size() >= limit) break;
            }
        }
        return result;
    }

    private List<Place> mergeLockedPlaces(CourseRegenerateRequest req, List<Place> aiPicked,
                                          List<Place> candidates, int placeCount) {
        Map<String, Place> byId = candidates.stream()
                .collect(Collectors.toMap(Place::getId, place -> place, (a, b) -> a, LinkedHashMap::new));
        List<Place> result = new ArrayList<>();
        Iterator<Place> aiIterator = aiPicked.iterator();

        if (req.getCurrentPlaces() != null) {
            for (CourseRegenerateRequest.CurrentPlace current : req.getCurrentPlaces()) {
                if (result.size() >= placeCount) break;
                if (current.isLocked()) {
                    Place locked = byId.get(current.getId());
                    if (locked != null && result.stream().noneMatch(p -> p.getId().equals(locked.getId()))) {
                        result.add(locked);
                    }
                    continue;
                }
                Place next = nextUnused(aiIterator, result);
                if (next != null) result.add(next);
            }
        }

        Random rng = new Random();
        List<Place> fallback = pickRandom(candidates, Math.min(placeCount, candidates.size()), rng);
        for (Place place : fallback) {
            if (result.size() >= placeCount) break;
            if (result.stream().noneMatch(p -> p.getId().equals(place.getId()))) {
                result.add(place);
            }
        }
        return result;
    }

    private Place nextUnused(Iterator<Place> iterator, List<Place> used) {
        while (iterator.hasNext()) {
            Place next = iterator.next();
            if (used.stream().noneMatch(p -> p.getId().equals(next.getId()))) {
                return next;
            }
        }
        return null;
    }

    private CourseResponse buildResponse(int index, List<Place> places, String travelMode) {
        String[] titles = {"코스 A", "코스 B", "코스 C"};
        return buildResponse(index, titles[Math.min(index, titles.length - 1)], places, travelMode);
    }

    private CourseResponse buildResponse(int index, String title, List<Place> places, String travelMode) {
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
                title,
                subTitle,
                formatMin(totalMinutes),
                "약 " + String.format("%,d", totalFare) + "원",
                stops
        );
    }

    private TransportInfoResponse calcTransport(Place from, Place to) {
        if (from.getLatitude() == null || from.getLongitude() == null
                || to.getLatitude() == null || to.getLongitude() == null) {
            return new TransportInfoResponse("20분", "15분", "10분", 5000);
        }
        double fLat = from.getLatitude().doubleValue(), fLng = from.getLongitude().doubleValue();
        double tLat = to.getLatitude().doubleValue(),   tLng = to.getLongitude().doubleValue();
        double dist = haversine(fLat, fLng, tLat, tLng);

        int walkTmap = tMapApiClient.getWalkMinutes(fLat, fLng, tLat, tLng);
        int walkMin = walkTmap > 0 ? walkTmap : (int) Math.round(dist / 4.0 * 60);

        int[] carInfo = tMapApiClient.getCarRouteInfo(fLat, fLng, tLat, tLng);
        int taxiMin = carInfo[0] > 0 ? carInfo[0] : Math.max(3, (int) Math.round(dist / 30.0 * 60));
        int fare    = carInfo[1] > 0 ? carInfo[1] : 3800 + (int) (dist * 800);

        int busMin = busService.estimateBusMinutes(fLat, fLng, tLat, tLng);

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
