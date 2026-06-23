package com.ssafy.layover.course;

import com.ssafy.layover.bus.BusService;
import com.ssafy.layover.tmap.TMapApiClient;
import com.ssafy.layover.place.Place;
import com.ssafy.layover.place.PlaceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private static final int RECOMMENDED_COURSE_COUNT = 3;
    private static final int FALLBACK_PICK_ATTEMPTS = 80;
    private static final String[] FALLBACK_TITLES = {"빠른 코스", "여유 코스", "딱 맞는 코스"};
    private static final double DAEJEON_LAT = 36.3316;
    private static final double DAEJEON_LNG = 127.4342;
    private static final double SEODDAEJEON_LAT = 36.3293;
    private static final double SEODDAEJEON_LNG = 127.4019;

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
        List<Place> candidates = selectCandidates(req.getThemeTags(), req.getDepartureStation(), req.getDurationMinutes());
        if (candidates.size() < 2) {
            candidates = placeMapper.findAllWithinRadius(stationCoord(req.getDepartureStation())[0],
                    stationCoord(req.getDepartureStation())[1], 7.0);
        }

        int durationMinutes = normalizedDuration(req.getDurationMinutes());
        int placeCount = placeCountFor(req.getDurationMinutes());
        Random rng = new Random();
        Map<String, TransportInfoResponse> transportCache = new HashMap<>();

        List<CourseResponse> results = new ArrayList<>();
        List<AiCourseClient.AiCoursePlan> aiPlans =
                aiCourseClient.recommendCourses(req, candidates, placeCount, RECOMMENDED_COURSE_COUNT, List.of());
        for (int i = 0; i < aiPlans.size() && results.size() < RECOMMENDED_COURSE_COUNT; i++) {
            List<Place> picked = placesByIds(aiPlans.get(i).placeIds(), candidates, placeCount);
            if (isValidCourse(picked, Math.min(placeCount, candidates.size()), req.getTravelMode(), durationMinutes, List.of(), req.getDepartureStation(), transportCache)) {
                results.add(buildResponse(results.size(), aiPlans.get(i).title(), picked, req.getTravelMode(), req.getDepartureStation()));
            }
        }

        while (results.size() < RECOMMENDED_COURSE_COUNT) {
            int index = results.size();
            List<Place> picked = pickTimeAware(
                    candidates,
                    placeCount,
                    rng,
                    req.getTravelMode(),
                    durationMinutes,
                    targetMinRatio(index),
                    targetMaxRatio(index),
                    List.of(),
                    req.getDepartureStation(),
                    transportCache
            );
            results.add(buildResponse(index, FALLBACK_TITLES[Math.min(index, FALLBACK_TITLES.length - 1)], picked, req.getTravelMode(), req.getDepartureStation()));
        }
        return results;
    }

    public CourseResponse regenerateCourse(CourseRegenerateRequest req) {
        List<Place> candidates = selectCandidates(req.getThemeTags(), req.getDepartureStation(), req.getDurationMinutes());
        if (candidates.size() < 2) {
            candidates = placeMapper.findAllWithinRadius(stationCoord(req.getDepartureStation())[0],
                    stationCoord(req.getDepartureStation())[1], 7.0);
        }

        int placeCount = req.getCurrentPlaces() != null && !req.getCurrentPlaces().isEmpty()
                ? req.getCurrentPlaces().size()
                : placeCountFor(req.getDurationMinutes());
        int durationMinutes = normalizedDuration(req.getDurationMinutes());
        Map<String, TransportInfoResponse> transportCache = new HashMap<>();

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

        if (!isValidCourse(aiPicked, Math.min(placeCount, candidates.size()), req.getTravelMode(), durationMinutes, lockedPlaceIds, req.getDepartureStation(), transportCache)) {
            aiPicked = pickTimeAware(
                    candidates,
                    placeCount,
                    new Random(),
                    req.getTravelMode(),
                    durationMinutes,
                    0.70,
                    0.85,
                    lockedPlaceIds,
                    req.getDepartureStation(),
                    transportCache
            );
        }

        List<Place> merged = mergeLockedPlaces(req, aiPicked, candidates, placeCount);
        merged = trimToFit(merged, req.getTravelMode(), durationMinutes, lockedPlaceIds, req.getDepartureStation(), transportCache);
        String title = aiPlans.isEmpty() ? "AI 재추천 코스" : aiPlans.get(0).title();
        return buildResponse(0, title, merged, req.getTravelMode(), req.getDepartureStation());
    }

    private List<Place> selectCandidates(List<String> themeTags, String departureStation, int durationMinutes) {
        double[] coord = stationCoord(departureStation);
        double lat = coord[0];
        double lng = coord[1];
        double radiusKm = durationMinutes <= 180 ? 4.0 : 7.0;

        if (themeTags == null || themeTags.isEmpty()) {
            return placeMapper.findAllWithinRadius(lat, lng, radiusKm);
        }
        return placeMapper.findByCategoryInWithinRadius(themeTags, lat, lng, radiusKm);
    }

    private double[] stationCoord(String departureStation) {
        String normalized = departureStation == null ? "" : departureStation.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("SEO") || normalized.contains("SEODDAEJEON")) {
            return new double[]{SEODDAEJEON_LAT, SEODDAEJEON_LNG};
        }
        return new double[]{DAEJEON_LAT, DAEJEON_LNG};
    }

    private int placeCountFor(int durationMinutes) {
        int count = durationMinutes / 50;
        return Math.max(1, Math.min(4, count));
    }

    private List<Place> pickRandom(List<Place> pool, int count, Random rng) {
        List<Place> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, rng);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private List<Place> pickTimeAware(List<Place> pool, int count, Random rng, String travelMode,
                                      int durationMinutes, double minRatio, double maxRatio,
                                      List<String> requiredPlaceIds,
                                      String departureStation,
                                      Map<String, TransportInfoResponse> transportCache) {
        if (pool == null || pool.isEmpty()) return List.of();

        int targetMin = (int) Math.floor(durationMinutes * minRatio);
        int targetMax = (int) Math.ceil(durationMinutes * maxRatio);
        List<Place> required = placesByIds(requiredPlaceIds, pool, count);
        List<Place> bestUnderLimit = new ArrayList<>(required);
        int bestScore = Integer.MAX_VALUE;

        for (int attempt = 0; attempt < FALLBACK_PICK_ATTEMPTS; attempt++) {
            List<Place> picked = new ArrayList<>(required);
            List<Place> shuffled = new ArrayList<>(pool);
            Collections.shuffle(shuffled, rng);

            for (Place place : shuffled) {
                if (picked.size() >= Math.min(count, pool.size())) break;
                if (picked.stream().noneMatch(p -> p.getId().equals(place.getId()))) {
                    picked.add(place);
                }
            }

            int minutes = estimateTotalMinutes(picked, travelMode, departureStation, transportCache);
            if (minutes <= durationMinutes) {
                int score = minutes >= targetMin && minutes <= targetMax
                        ? 0
                        : Math.min(Math.abs(minutes - targetMin), Math.abs(minutes - targetMax));
                if (score < bestScore) {
                    bestScore = score;
                    bestUnderLimit = picked;
                }
                if (score == 0) {
                    return picked;
                }
            }
        }

        if (!bestUnderLimit.isEmpty()) {
            return bestUnderLimit;
        }
        return trimToFit(pickRandom(pool, count, rng), travelMode, durationMinutes, requiredPlaceIds, departureStation, transportCache);
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

    private boolean isValidCourse(List<Place> places, int placeCount, String travelMode, int durationMinutes,
                                  List<String> requiredPlaceIds,
                                  String departureStation,
                                  Map<String, TransportInfoResponse> transportCache) {
        if (places == null || places.isEmpty()) return false;
        if (places.size() < Math.max(1, placeCount)) return false;

        Set<String> uniqueIds = new HashSet<>();
        for (Place place : places) {
            if (place == null || place.getId() == null || !uniqueIds.add(place.getId())) {
                return false;
            }
        }

        if (requiredPlaceIds != null) {
            for (String requiredId : requiredPlaceIds) {
                if (requiredId != null && !uniqueIds.contains(requiredId)) {
                    return false;
                }
            }
        }

        return estimateTotalMinutes(places, travelMode, departureStation, transportCache) <= durationMinutes;
    }

    private List<Place> trimToFit(List<Place> places, String travelMode, int durationMinutes,
                                  List<String> lockedPlaceIds,
                                  String departureStation,
                                  Map<String, TransportInfoResponse> transportCache) {
        List<Place> result = new ArrayList<>(places == null ? List.of() : places);
        Set<String> locked = lockedPlaceIds == null ? Set.of() : new HashSet<>(lockedPlaceIds);

        while (result.size() > 1 && estimateTotalMinutes(result, travelMode, departureStation, transportCache) > durationMinutes) {
            int removableIndex = -1;
            for (int i = result.size() - 1; i >= 0; i--) {
                Place place = result.get(i);
                if (place.getId() == null || !locked.contains(place.getId())) {
                    removableIndex = i;
                    break;
                }
            }
            if (removableIndex < 0) break;
            result.remove(removableIndex);
        }
        return result;
    }

    private int estimateTotalMinutes(List<Place> places, String travelMode, String departureStation,
                                     Map<String, TransportInfoResponse> transportCache) {
        if (places == null || places.isEmpty()) return 0;
        int total = 0;
        Place station = stationPlace(departureStation);
        total += travelMinutes(station, places.get(0), travelMode, transportCache);

        for (int i = 0; i < places.size(); i++) {
            Place cur = places.get(i);
            total += parseMin(stayTimeFor(cur.getCategory()));

            if (i < places.size() - 1) {
                total += travelMinutes(cur, places.get(i + 1), travelMode, transportCache);
            }
        }
        total += travelMinutes(places.get(places.size() - 1), station, travelMode, transportCache);
        return total;
    }

    private int travelMinutes(Place from, Place to, String travelMode, Map<String, TransportInfoResponse> transportCache) {
        TransportInfoResponse transport = cachedTransport(from, to, transportCache);
        boolean isWalk = "WALK".equals(travelMode);
        return parseMin(isWalk ? transport.getWalkTime() : transport.getTaxiTime());
    }

    private TransportInfoResponse cachedTransport(Place from, Place to, Map<String, TransportInfoResponse> cache) {
        String key = from.getId() + "->" + to.getId();
        return cache.computeIfAbsent(key, ignored -> calcTransport(from, to));
    }

    private Place stationPlace(String departureStation) {
        String normalized = departureStation == null ? "" : departureStation.trim().toUpperCase(Locale.ROOT);
        Place station = new Place();
        if (normalized.contains("SEO") || normalized.contains("SEODDAEJEON") || normalized.contains("서대전")) {
            station.setId("__STATION_SEODDAEJEON__");
            station.setName("서대전역");
            station.setLatitude(BigDecimal.valueOf(36.3226));
            station.setLongitude(BigDecimal.valueOf(127.4039));
        } else {
            station.setId("__STATION_DAEJEON__");
            station.setName("대전역");
            station.setLatitude(BigDecimal.valueOf(36.3325));
            station.setLongitude(BigDecimal.valueOf(127.4348));
        }
        station.setCategory("STATION");
        return station;
    }

    private int normalizedDuration(int durationMinutes) {
        return durationMinutes > 0 ? durationMinutes : 120;
    }

    private double targetMinRatio(int index) {
        return switch (index) {
            case 0 -> 0.50;
            case 1 -> 0.65;
            default -> 0.80;
        };
    }

    private double targetMaxRatio(int index) {
        return switch (index) {
            case 0 -> 0.60;
            case 1 -> 0.75;
            default -> 0.90;
        };
    }

    private CourseResponse buildResponse(int index, List<Place> places, String travelMode) {
        return buildResponse(index, FALLBACK_TITLES[Math.min(index, FALLBACK_TITLES.length - 1)], places, travelMode, null);
    }

    private CourseResponse buildResponse(int index, String title, List<Place> places, String travelMode, String departureStation) {
        List<CourseStopResponse> stops = new ArrayList<>();
        int totalMinutes = 0;
        int totalFare = 0;
        Map<String, TransportInfoResponse> transportCache = new HashMap<>();
        Place station = stationPlace(departureStation);

        TransportInfoResponse departureTransport = null;
        if (!places.isEmpty()) {
            departureTransport = cachedTransport(station, places.get(0), transportCache);
            totalMinutes += parseMin("WALK".equals(travelMode) ? departureTransport.getWalkTime() : departureTransport.getTaxiTime());
            if (!"WALK".equals(travelMode)) totalFare += departureTransport.getTaxiFare();
        }
        stops.add(CourseStopResponse.ofStation(station.getName(),
                station.getLatitude().doubleValue(), station.getLongitude().doubleValue(),
                departureTransport, travelMode));

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
            } else {
                transport = cachedTransport(cur, station, transportCache);
                boolean isWalk = "WALK".equals(travelMode);
                totalMinutes += parseMin(isWalk ? transport.getWalkTime() : transport.getTaxiTime());
                if (!isWalk) totalFare += transport.getTaxiFare();
            }
            stops.add(new CourseStopResponse(cur, stayTime, transport, travelMode));
        }

        stops.add(CourseStopResponse.ofStation(station.getName(),
                station.getLatitude().doubleValue(), station.getLongitude().doubleValue(),
                null, travelMode));

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
            case "FOOD"    -> "45분";
            case "CAFE"    -> "20분";
            case "NATURE"  -> "40분";
            case "CULTURE" -> "30분";
            default        -> "30분";
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
