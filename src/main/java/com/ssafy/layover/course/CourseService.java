package com.ssafy.layover.course;

import com.ssafy.layover.bus.BusService;
import com.ssafy.layover.tmap.TMapApiClient;
import com.ssafy.layover.place.Place;
import com.ssafy.layover.place.PlaceMapper;
import com.ssafy.layover.place.StationPlaceSeeder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseService {

    private static final int RECOMMENDED_COURSE_COUNT = 3;
    private static final int FALLBACK_PICK_ATTEMPTS = 15;
    private static final String[] FALLBACK_TITLES = {"빠른 코스", "여유 코스", "딱 맞는 코스"};

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
        List<Place> allCandidates = selectCandidates(req.getThemeTags());
        if (allCandidates.size() < 2) {
            allCandidates = placeMapper.findAllWithLocation();
        }

        int durationMinutes = normalizedDuration(req.getDurationMinutes());
        int extendedDuration = durationMinutes + 60;
        int placeCount = placeCountFor(durationMinutes);
        int extendedPlaceCount = placeCountFor(extendedDuration);

        List<Place> candidates = filterCandidatesByStationRadius(
                allCandidates, req.getDepartureStation(), durationMinutes, placeCount, req.getTravelMode());
        List<Place> extendedCandidates = filterCandidatesByStationRadius(
                allCandidates, req.getDepartureStation(), extendedDuration, extendedPlaceCount, req.getTravelMode());
        Random rng = new Random();

        List<CourseResponse> results = new ArrayList<>();
        List<List<Place>> addedPlaceLists = new ArrayList<>();

        List<AiCourseClient.AiCoursePlan> aiPlans = aiCourseClient.recommendCourses(
                req, candidates, extendedCandidates, placeCount, extendedPlaceCount, RECOMMENDED_COURSE_COUNT, List.of());
        if (aiCourseClient.isBlocked()) {
            throw new IllegalStateException("AI 코스 추천 호출에 실패했습니다.");
        }

        // 코스 1, 2: 표준 예산 + 카테고리 제약 + 코스 간 중복 방지
        for (int i = 0; i < Math.min(2, aiPlans.size()) && addedPlaceLists.size() < 2; i++) {
            List<Place> picked = placesByIds(aiPlans.get(i).placeIds(), candidates, placeCount);
            boolean isValid = isValidCourse(picked, Math.min(placeCount, candidates.size()), req.getTravelMode(), durationMinutes, List.of(), req.getDepartureStation());
            boolean categoryOK = isValid && hasValidCategoryConstraints(picked);
            boolean distinct = categoryOK && isDistinctFrom(picked, addedPlaceLists);
            log.info("[Course] AI 플랜 {} 검증 - isValid:{} categoryOK:{} distinct:{} picked:{}",
                    i, isValid, categoryOK, distinct, picked.stream().map(p -> p.getName() + "/" + p.getCategory()).toList());
            if (isValid && categoryOK && distinct) {
                results.add(buildResponse(results.size(), aiPlans.get(i).title(), picked, req.getTravelMode(), req.getDepartureStation()));
                addedPlaceLists.add(new ArrayList<>(picked));
            }
        }

        // 코스 1, 2 폴백
        while (addedPlaceLists.size() < 2) {
            int index = addedPlaceLists.size();
            log.info("[Course] 코스 {}: AI 플랜 통과 실패, 폴백 진입", index);
            List<Place> picked = pickCategoryAware(candidates, placeCount, rng, req.getTravelMode(), durationMinutes,
                    targetMinRatio(index), targetMaxRatio(index), List.of(), req.getDepartureStation(), addedPlaceLists);
            results.add(buildResponse(results.size(), FALLBACK_TITLES[Math.min(index, FALLBACK_TITLES.length - 1)], picked, req.getTravelMode(), req.getDepartureStation()));
            addedPlaceLists.add(new ArrayList<>(picked));
        }

        // 코스 3: 확장 예산 (durationMinutes + 60)
        AiCourseClient.AiCoursePlan extendedPlan = aiPlans.size() >= 3 ? aiPlans.get(2) : null;
        List<Place> extendedPicked = null;
        if (extendedPlan != null) {
            extendedPicked = placesByIds(extendedPlan.placeIds(), extendedCandidates, extendedPlaceCount);
            if (!isValidCourse(extendedPicked, 1, req.getTravelMode(), extendedDuration, List.of(), req.getDepartureStation())) {
                extendedPicked = null;
            }
        }
        if (extendedPicked == null || extendedPicked.isEmpty()) {
            extendedPicked = pickTimeAware(extendedCandidates, extendedPlaceCount, rng, req.getTravelMode(),
                    extendedDuration, 0.80, 0.95, List.of(), req.getDepartureStation());
        }
        String extendedTitle = (extendedPlan != null && extendedPlan.title() != null && !extendedPlan.title().isBlank())
                ? extendedPlan.title() : FALLBACK_TITLES[2];
        results.add(buildResponse(2, extendedTitle, extendedPicked, req.getTravelMode(), req.getDepartureStation()));

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
        int durationMinutes = normalizedDuration(req.getDurationMinutes());
        candidates = filterCandidatesByStationRadius(candidates, req.getDepartureStation(), durationMinutes, placeCount, req.getTravelMode());

        List<String> lockedPlaceIds = req.getCurrentPlaces() == null
                ? List.of()
                : req.getCurrentPlaces().stream()
                .filter(CourseRegenerateRequest.CurrentPlace::isLocked)
                .map(CourseRegenerateRequest.CurrentPlace::getId)
                .filter(Objects::nonNull)
                .toList();

        List<AiCourseClient.AiCoursePlan> aiPlans =
                aiCourseClient.recommendCourses(req, candidates, placeCount, 1, lockedPlaceIds);
        if (aiCourseClient.isBlocked()) {
            throw new IllegalStateException("AI 코스 추천 호출에 실패했습니다.");
        }
        List<Place> aiPicked = aiPlans.isEmpty()
                ? List.of()
                : placesByIds(aiPlans.get(0).placeIds(), candidates, placeCount);

        if (!isValidCourse(aiPicked, Math.min(placeCount, candidates.size()), req.getTravelMode(), durationMinutes, lockedPlaceIds, req.getDepartureStation())) {
            aiPicked = pickTimeAware(
                    candidates,
                    placeCount,
                    new Random(),
                    req.getTravelMode(),
                    durationMinutes,
                    0.70,
                    0.85,
                    lockedPlaceIds,
                    req.getDepartureStation()
            );
        }

        List<Place> merged = mergeLockedPlaces(req, aiPicked, candidates, placeCount);
        merged = trimToFit(merged, req.getTravelMode(), durationMinutes, lockedPlaceIds, req.getDepartureStation());
        String title = aiPlans.isEmpty() ? "AI 재추천 코스" : aiPlans.get(0).title();
        return buildResponse(0, title, merged, req.getTravelMode(), req.getDepartureStation());
    }

    public CourseResponse recalculateCourse(CourseRecalculateRequest req) {
        if (req.getPlaceIds() == null || req.getPlaceIds().isEmpty()) {
            return buildResponse(0, req.getTitle() == null ? "편집 코스" : req.getTitle(),
                    List.of(), req.getTravelMode(), req.getDepartureStation());
        }

        List<Place> places = req.getPlaceIds().stream()
                .filter(Objects::nonNull)
                .filter(id -> !isStationPlaceId(id))
                .map(placeMapper::findById)
                .filter(Objects::nonNull)
                .toList();

        String title = req.getTitle() == null || req.getTitle().isBlank()
                ? "편집 코스"
                : req.getTitle();
        return buildResponse(0, title, places, req.getTravelMode(), req.getDepartureStation(), true);
    }

    private List<Place> selectCandidates(List<String> themeTags) {
        if (themeTags == null || themeTags.isEmpty()) {
            return placeMapper.findAllWithLocation();
        }
        List<String> categories = expandThemeTags(themeTags);
        return categories.isEmpty()
                ? placeMapper.findAllWithLocation()
                : placeMapper.findByCategoryIn(categories);
    }

    private List<String> expandThemeTags(List<String> themeTags) {
        Set<String> categories = new LinkedHashSet<>();
        for (String rawTag : themeTags) {
            if (rawTag == null || rawTag.isBlank()) continue;
            String tag = rawTag.trim().toUpperCase(Locale.ROOT);
            switch (tag) {
                case "FOOD" -> categories.add("FOOD");
                case "CAFE" -> {
                    categories.add("CAFE");
                    categories.add("FOOD");
                }
                case "NATURE" -> {
                    categories.add("NATURE");
                    categories.add("TOUR");
                    categories.add("LEPORTS");
                }
                case "TOUR" -> categories.add("TOUR");
                case "CULTURE" -> categories.add("CULTURE");
                case "SHOPPING" -> categories.add("SHOPPING");
                case "FESTIVAL" -> categories.add("FESTIVAL");
                case "LEPORTS" -> categories.add("LEPORTS");
                default -> categories.add(tag);
            }
        }
        return new ArrayList<>(categories);
    }

    private List<Place> filterCandidatesByStationRadius(List<Place> candidates, String departureStation,
                                                        int durationMinutes, int placeCount, String travelMode) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        Place station = stationPlace(departureStation);
        double radiusKm = radiusKmFor(durationMinutes, "WALK".equalsIgnoreCase(travelMode));
        List<Place> sortedByStationDistance = candidates.stream()
                .filter(this::hasLocation)
                .sorted(Comparator.comparingDouble(place -> distanceFromStation(station, place)))
                .toList();

        List<Place> withinRadius = sortedByStationDistance.stream()
                .filter(place -> distanceFromStation(station, place) <= radiusKm)
                .toList();

        int minimumNeeded = Math.max(placeCount * RECOMMENDED_COURSE_COUNT, 8);
        if (withinRadius.size() >= Math.min(minimumNeeded, sortedByStationDistance.size())) {
            return withinRadius;
        }
        if (withinRadius.size() >= Math.max(placeCount, 2)) {
            return withinRadius;
        }
        return sortedByStationDistance.isEmpty() ? candidates : sortedByStationDistance;
    }

    private boolean hasLocation(Place place) {
        return place != null && place.getLatitude() != null && place.getLongitude() != null;
    }

    private double distanceFromStation(Place station, Place place) {
        if (!hasLocation(station) || !hasLocation(place)) {
            return Double.MAX_VALUE;
        }
        return haversine(
                station.getLatitude().doubleValue(),
                station.getLongitude().doubleValue(),
                place.getLatitude().doubleValue(),
                place.getLongitude().doubleValue()
        );
    }

    private double radiusKmFor(int durationMinutes, boolean walkOnly) {
        if (walkOnly) {
            if (durationMinutes <= 180) return 2.0;
            if (durationMinutes <= 300) return 4.0;
            return 6.0;
        }

        if (durationMinutes <= 180) return 3.0;
        if (durationMinutes <= 300) return 7.0;
        return 12.0;
    }

    private int placeCountFor(int durationMinutes) {
        if (durationMinutes <= 90)  return 2;
        if (durationMinutes <= 210) return 3;
        return 4;
    }

    private List<Place> pickRandom(List<Place> pool, int count, Random rng) {
        List<Place> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, rng);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private List<Place> pickTimeAware(List<Place> pool, int count, Random rng, String travelMode,
                                      int durationMinutes, double minRatio, double maxRatio,
                                      List<String> requiredPlaceIds,
                                      String departureStation) {
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

            int minutes = estimateTotalMinutes(picked, travelMode, departureStation);
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
        log.warn("[Course] pickTimeAware 폴백 발동: {}번 시도 전부 실패, pickRandom으로 대체", FALLBACK_PICK_ATTEMPTS);
        return removeCategoryViolations(
                trimToFit(pickRandom(pool, count, rng), travelMode, durationMinutes, requiredPlaceIds, departureStation));
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
                                  String departureStation) {
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

        return estimateTotalMinutes(places, travelMode, departureStation) <= durationMinutes;
    }

    private List<Place> trimToFit(List<Place> places, String travelMode, int durationMinutes,
                                  List<String> lockedPlaceIds,
                                  String departureStation) {
        List<Place> result = new ArrayList<>(places == null ? List.of() : places);
        Set<String> locked = lockedPlaceIds == null ? Set.of() : new HashSet<>(lockedPlaceIds);

        while (result.size() > 1 && estimateTotalMinutes(result, travelMode, departureStation) > durationMinutes) {
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

    private int estimateTotalMinutes(List<Place> places, String travelMode, String departureStation) {
        if (places == null || places.isEmpty()) return 0;
        int total = 0;
        Place station = stationPlace(departureStation);
        total += estimateTravelMinutes(station, places.get(0), travelMode);

        for (int i = 0; i < places.size(); i++) {
            Place cur = places.get(i);
            total += parseMin(stayTimeFor(cur.getCategory()));

            if (i < places.size() - 1) {
                total += estimateTravelMinutes(cur, places.get(i + 1), travelMode);
            }
        }
        total += estimateTravelMinutes(places.get(places.size() - 1), station, travelMode);
        return total;
    }

    private int estimateTravelMinutes(Place from, Place to, String travelMode) {
        if (from.getLatitude() == null || from.getLongitude() == null
                || to.getLatitude() == null || to.getLongitude() == null) {
            return "WALK".equals(travelMode) ? 20 : 10;
        }

        double dist = haversine(
                from.getLatitude().doubleValue(),
                from.getLongitude().doubleValue(),
                to.getLatitude().doubleValue(),
                to.getLongitude().doubleValue()
        );
        // ×1.3 보정: Haversine 직선거리 대비 실제 도로 경로는 평균 1.3배 길므로
        return "WALK".equals(travelMode)
                ? Math.max(3, (int) Math.ceil(dist * 1.3 / 4.0 * 60))
                : Math.max(3, (int) Math.ceil(dist * 1.3 / 28.0 * 60));
    }

    private TransportInfoResponse cachedTransport(Place from, Place to, String travelMode, Map<String, TransportInfoResponse> cache) {
        String key = travelMode + ":" + from.getId() + "->" + to.getId();
        return cache.computeIfAbsent(key, ignored -> calcTransport(from, to, travelMode));
    }

    private TransportInfoResponse cachedTransport(Place from, Place to, String travelMode,
                                                  Map<String, TransportInfoResponse> cache,
                                                  boolean calculateAllTmapModes) {
        String key = travelMode + ":" + calculateAllTmapModes + ":" + from.getId() + "->" + to.getId();
        return cache.computeIfAbsent(key, ignored -> calcTransport(from, to, travelMode, calculateAllTmapModes));
    }

    private List<Place> removeCategoryViolations(List<Place> places) {
        if (places == null) return List.of();
        List<Place> result = new ArrayList<>();
        for (Place place : places) {
            List<Place> candidate = new ArrayList<>(result);
            candidate.add(place);
            if (hasValidCategoryConstraints(candidate)) {
                result.add(place);
            }
        }
        return result;
    }

    private boolean hasValidCategoryConstraints(List<Place> places) {
        if (places == null) return true;
        int foodCount = 0, cafeCount = 0;
        String prevCategory = null;
        for (Place p : places) {
            String cat = p.getCategory() == null ? "" : p.getCategory();
            if (cat.equals(prevCategory)) return false;
            if ("FOOD".equals(cat) && ++foodCount > 1) return false;
            if ("CAFE".equals(cat) && ++cafeCount > 2) return false;
            prevCategory = cat;
        }
        return true;
    }

    private boolean isDistinctFrom(List<Place> course, List<List<Place>> existingCourses) {
        if (existingCourses == null || existingCourses.isEmpty()) return true;
        Set<String> courseIds = course.stream().map(Place::getId).collect(Collectors.toSet());
        for (List<Place> existing : existingCourses) {
            Set<String> existingIds = existing.stream().map(Place::getId).collect(Collectors.toSet());
            long overlap = courseIds.stream().filter(existingIds::contains).count();
            int minSize = Math.min(courseIds.size(), existingIds.size());
            if (minSize > 0 && (double) overlap / minSize >= 0.5) return false;
        }
        return true;
    }

    private List<Place> pickCategoryAware(List<Place> pool, int count, Random rng, String travelMode,
                                           int durationMinutes, double minRatio, double maxRatio,
                                           List<String> requiredPlaceIds, String departureStation,
                                           List<List<Place>> existingCourseLists) {
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
                if (picked.stream().anyMatch(p -> p.getId().equals(place.getId()))) continue;
                List<Place> candidate = new ArrayList<>(picked);
                candidate.add(place);
                if (hasValidCategoryConstraints(candidate)) {
                    picked.add(place);
                }
            }

            if (!isDistinctFrom(picked, existingCourseLists)) continue;

            int minutes = estimateTotalMinutes(picked, travelMode, departureStation);
            if (minutes <= durationMinutes) {
                int score = minutes >= targetMin && minutes <= targetMax
                        ? 0
                        : Math.min(Math.abs(minutes - targetMin), Math.abs(minutes - targetMax));
                if (score < bestScore) {
                    bestScore = score;
                    bestUnderLimit = picked;
                }
                if (score == 0) return picked;
            }
        }

        if (!bestUnderLimit.isEmpty()) return bestUnderLimit;
        log.warn("[Course] pickCategoryAware 폴백 발동: {}번 시도 전부 실패, pickRandom으로 대체", FALLBACK_PICK_ATTEMPTS);
        return removeCategoryViolations(
                trimToFit(pickRandom(pool, count, rng), travelMode, durationMinutes, requiredPlaceIds, departureStation));
    }

    private Place stationPlace(String departureStation) {
        String normalized = departureStation == null ? "" : departureStation.trim().toUpperCase(Locale.ROOT);
        Place station = new Place();
        if (normalized.contains("SINTANJIN") || normalized.contains("신탄진")) {
            station.setId(StationPlaceSeeder.SINTANJIN_STATION_ID);
            station.setName("신탄진역");
            station.setLatitude(BigDecimal.valueOf(36.4518));
            station.setLongitude(BigDecimal.valueOf(127.4297));
        } else if (normalized.contains("SEO") || normalized.contains("SEODAEJEON")
                || normalized.contains("SEODDAEJEON") || normalized.contains("서대전")) {
            station.setId(StationPlaceSeeder.SEODAEJEON_STATION_ID);
            station.setName("서대전역");
            station.setLatitude(BigDecimal.valueOf(36.3226));
            station.setLongitude(BigDecimal.valueOf(127.4039));
        } else {
            station.setId(StationPlaceSeeder.DAEJEON_STATION_ID);
            station.setName("대전역");
            station.setLatitude(BigDecimal.valueOf(36.3325));
            station.setLongitude(BigDecimal.valueOf(127.4348));
        }
        station.setCategory("STATION");
        return station;
    }

    private boolean isStationPlaceId(String id) {
        return id != null && (id.startsWith("__STATION") || id.startsWith("STATION_"));
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
        return buildResponse(index, title, places, travelMode, departureStation, false);
    }

    private CourseResponse buildResponse(int index, String title, List<Place> places, String travelMode,
                                         String departureStation, boolean calculateAllTmapModes) {
        List<CourseStopResponse> stops = new ArrayList<>();
        int totalMinutes = 0;
        int totalFare = 0;
        Map<String, TransportInfoResponse> transportCache = new HashMap<>();
        Place station = stationPlace(departureStation);

        TransportInfoResponse departureTransport = null;
        TransportInfoResponse returnTransport = null;
        if (!places.isEmpty()) {
            departureTransport = cachedTransport(station, places.get(0), travelMode, transportCache, calculateAllTmapModes);
            totalMinutes += parseMin("WALK".equals(travelMode) ? departureTransport.getWalkTime() : departureTransport.getTaxiTime());
            if (!"WALK".equals(travelMode)) totalFare += departureTransport.getTaxiFare();
            stops.add(new CourseStopResponse(station, "0분", departureTransport, travelMode));
        }

        for (int i = 0; i < places.size(); i++) {
            Place cur = places.get(i);
            String stayTime = stayTimeFor(cur.getCategory());
            totalMinutes += parseMin(stayTime);

            TransportInfoResponse transport = null;
            if (i < places.size() - 1) {
                transport = cachedTransport(cur, places.get(i + 1), travelMode, transportCache, calculateAllTmapModes);
                boolean isWalk = "WALK".equals(travelMode);
                totalMinutes += parseMin(isWalk ? transport.getWalkTime() : transport.getTaxiTime());
                if (!isWalk) totalFare += transport.getTaxiFare();
            }
            if (i == places.size() - 1) {
                returnTransport = cachedTransport(cur, station, travelMode, transportCache, calculateAllTmapModes);
                boolean isWalk = "WALK".equals(travelMode);
                totalMinutes += parseMin(isWalk ? returnTransport.getWalkTime() : returnTransport.getTaxiTime());
                if (!isWalk) totalFare += returnTransport.getTaxiFare();
                transport = returnTransport;
            }
            stops.add(new CourseStopResponse(cur, stayTime, transport, travelMode));
        }

        if (!places.isEmpty()) {
            stops.add(new CourseStopResponse(station, "0분", null, travelMode));
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

    private TransportInfoResponse calcTransport(Place from, Place to, String travelMode) {
        return calcTransport(from, to, travelMode, false);
    }

    private TransportInfoResponse calcTransport(Place from, Place to, String travelMode, boolean calculateAllTmapModes) {
        if (from.getLatitude() == null || from.getLongitude() == null
                || to.getLatitude() == null || to.getLongitude() == null) {
            return new TransportInfoResponse("20분", "15분", "10분", 5000);
        }
        double fLat = from.getLatitude().doubleValue(), fLng = from.getLongitude().doubleValue();
        double tLat = to.getLatitude().doubleValue(),   tLng = to.getLongitude().doubleValue();
        double dist = haversine(fLat, fLng, tLat, tLng);

        int estimatedWalkMin = Math.max(3, (int) Math.ceil(dist / 4.0 * 60));
        int estimatedTaxiMin = Math.max(3, (int) Math.ceil(dist / 28.0 * 60));
        int estimatedFare = 3800 + (int) (dist * 800);

        int walkMin = estimatedWalkMin;
        int taxiMin = estimatedTaxiMin;
        int fare = estimatedFare;

        if (calculateAllTmapModes || "WALK".equals(travelMode)) {
            int walkTmap = tMapApiClient.getWalkMinutes(fLat, fLng, tLat, tLng);
            walkMin = walkTmap > 0 ? walkTmap : estimatedWalkMin;
        }
        if (calculateAllTmapModes || !"WALK".equals(travelMode)) {
            int[] carInfo = tMapApiClient.getCarRouteInfo(fLat, fLng, tLat, tLng);
            taxiMin = carInfo[0] > 0 ? carInfo[0] : estimatedTaxiMin;
            fare = carInfo[1] > 0 ? carInfo[1] : estimatedFare;
        }

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
