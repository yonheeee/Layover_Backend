package com.ssafy.layover.course;

import com.ssafy.layover.bus.BusService;
import com.ssafy.layover.common.exception.NotFoundException;
import com.ssafy.layover.kakao.KakaoRouteApiClient;
import com.ssafy.layover.place.Place;
import com.ssafy.layover.place.PlaceMapper;
import com.ssafy.layover.place.StationPlaceSeeder;
import com.ssafy.layover.weather.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private static final String[] FALLBACK_TITLES = {"추천 코스 A", "추천 코스 B", "플러스+1 코스"};

    /** 선택한 테마에서 이 개수 미만이면 전체 장소로 확장한다. */
    private static final int MIN_CANDIDATES_FOR_THEME = 6;

    /** 후보가 부족할 때 허용하는 반경 완화 배수. 무제한 확장을 막는다. */
    private static final double RADIUS_RELAX_MULTIPLIER = 1.5;

    /** 복귀 버퍼를 빼고도 최소한 이만큼은 코스에 쓸 수 있어야 한다. */
    private static final int MIN_USABLE_MINUTES = 30;

    private final PlaceMapper placeMapper;
    private final CourseMapper courseMapper;
    private final CoursePlaceMapper coursePlaceMapper;
    private final BusService busService;
    private final KakaoRouteApiClient kakaoRouteApiClient;
    private final AiCourseClient aiCourseClient;
    private final WeatherService weatherService;

    /**
     * 열차 복귀를 위해 남겨두는 여유 시간(분).
     *
     * <p>예전에는 프론트엔드가 열차 모드에서만 30분을 빼서 보내고, 직접 입력 모드에서는 빼지 않았다.
     * 게다가 백엔드는 이 값을 응답에 실어 보내기만 하고 시간 예산 계산에는 반영하지 않아서,
     * AI가 만든 코스는 잔여 시간을 100% 쓰면서도 "복귀 30분 권장"이라고 표시될 수 있었다.
     * 이제 버퍼는 백엔드 한 곳에서만 적용한다.
     */
    @Value("${course.return-buffer-minutes:30}")
    private int returnBufferMinutes;

    public CourseService(PlaceMapper placeMapper, CourseMapper courseMapper,
                         CoursePlaceMapper coursePlaceMapper, BusService busService,
                         KakaoRouteApiClient kakaoRouteApiClient,
                         AiCourseClient aiCourseClient,
                         WeatherService weatherService) {
        this.placeMapper = placeMapper;
        this.courseMapper = courseMapper;
        this.coursePlaceMapper = coursePlaceMapper;
        this.busService = busService;
        this.kakaoRouteApiClient = kakaoRouteApiClient;
        this.aiCourseClient = aiCourseClient;
        this.weatherService = weatherService;
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
            throw new NotFoundException("코스를 찾을 수 없거나 권한이 없습니다.");
        }
        coursePlaceMapper.deleteByCourseId(courseId);
        courseMapper.deleteById(courseId);
    }

    public List<CourseResponse> generateCourses(CourseGenerateRequest req) {
        applyWeatherIfMissing(req);

        List<Place> allCandidates = selectCandidates(req.getThemeTags());
        if (allCandidates.size() < 2) {
            allCandidates = placeMapper.findAllWithLocation();
        }

        int durationMinutes = normalizedDuration(req.getDurationMinutes());
        int extendedDuration = durationMinutes + 60;
        // 복귀 버퍼를 뺀 실제 편성 가능 시간. 검증/선정은 모두 이 값을 기준으로 한다.
        int usableDuration = usableMinutes(durationMinutes);
        int usableExtended = usableMinutes(extendedDuration);
        int placeCount = placeCountFor(usableDuration);
        int extendedPlaceCount = placeCountFor(usableExtended);
        log.info("[Course] 잔여 {}분 - 복귀 버퍼 {}분 = 편성 가능 {}분", durationMinutes, returnBufferMinutes, usableDuration);

        List<Place> candidates = filterCandidatesByStationRadius(
                allCandidates, req.getDepartureStation(), usableDuration, placeCount, req.getTravelMode());
        List<Place> extendedCandidates = filterCandidatesByStationRadius(
                allCandidates, req.getDepartureStation(), usableExtended, extendedPlaceCount, req.getTravelMode());
        Random rng = new Random();

        List<CourseResponse> results = new ArrayList<>();
        List<List<Place>> addedPlaceLists = new ArrayList<>();

        List<AiCourseClient.AiCoursePlan> aiPlans = aiCourseClient.recommendCourses(
                req, candidates, extendedCandidates, placeCount, extendedPlaceCount, RECOMMENDED_COURSE_COUNT,
                List.of(), usableDuration, usableExtended);
        // AI 호출이 실패해도 코스 추천 자체를 실패시키지 않는다.
        // 아래 폴백 경로가 시간 예산과 카테고리 규칙으로 동일한 품질 기준의 코스를 만들어 내며,
        // 응답의 fallbackUsed 플래그로 프론트에 "규칙 보정"임을 정직하게 노출한다.
        if (aiPlans.isEmpty()) {
            log.warn("[Course] AI 추천 결과가 없어 규칙 기반 폴백으로 코스를 생성합니다. (aiBlocked={})",
                    aiCourseClient.isBlocked());
        }

        // 코스 1, 2: 표준 예산 + 카테고리 제약 + 코스 간 중복 방지
        for (int i = 0; i < Math.min(2, aiPlans.size()) && addedPlaceLists.size() < 2; i++) {
            List<Place> picked = placesByIds(aiPlans.get(i).placeIds(), candidates, placeCount);
            boolean isValid = isValidCourse(picked, Math.min(placeCount, candidates.size()), req.getTravelMode(), usableDuration, List.of(), req.getDepartureStation());
            boolean categoryOK = isValid && hasValidCategoryConstraints(picked);
            boolean distinct = categoryOK && isDistinctFrom(picked, addedPlaceLists);
            log.info("[Course] AI 플랜 {} 검증 - isValid:{} categoryOK:{} distinct:{} picked:{}",
                    i, isValid, categoryOK, distinct, picked.stream().map(p -> p.getName() + "/" + p.getCategory()).toList());
            if (isValid && categoryOK && distinct) {
                results.add(buildResponse(results.size(), safeTitle(aiPlans.get(i).title(), i), picked,
                        req.getTravelMode(), req.getDepartureStation(), false, durationMinutes, false));
                addedPlaceLists.add(new ArrayList<>(picked));
            }
        }

        // 코스 1, 2 폴백
        while (addedPlaceLists.size() < 2) {
            int index = addedPlaceLists.size();
            log.info("[Course] 코스 {}: AI 플랜 통과 실패, 폴백 진입", index);
            List<Place> picked = pickCategoryAware(candidates, placeCount, rng, req.getTravelMode(), usableDuration,
                    targetMinRatio(index), targetMaxRatio(index), List.of(), req.getDepartureStation(), addedPlaceLists);
            results.add(buildResponse(results.size(), FALLBACK_TITLES[Math.min(index, FALLBACK_TITLES.length - 1)], picked,
                    req.getTravelMode(), req.getDepartureStation(), false, durationMinutes, true));
            addedPlaceLists.add(new ArrayList<>(picked));
        }

        // 코스 3: 확장 예산 (durationMinutes + 60)
        AiCourseClient.AiCoursePlan extendedPlan = aiPlans.size() >= 3 ? aiPlans.get(2) : null;
        List<Place> extendedPicked = null;
        boolean extendedFallbackUsed = extendedPlan == null;
        if (extendedPlan != null) {
            extendedPicked = placesByIds(extendedPlan.placeIds(), extendedCandidates, extendedPlaceCount);
            if (!isValidCourse(extendedPicked, 1, req.getTravelMode(), usableExtended, List.of(), req.getDepartureStation())) {
                extendedPicked = null;
                extendedFallbackUsed = true;
            }
        }
        if (extendedPicked == null || extendedPicked.isEmpty()) {
            extendedPicked = pickTimeAware(extendedCandidates, extendedPlaceCount, rng, req.getTravelMode(),
                    usableExtended, 0.80, 0.95, List.of(), req.getDepartureStation());
            extendedFallbackUsed = true;
        }
        String extendedTitle = (extendedPlan != null && extendedPlan.title() != null && !extendedPlan.title().isBlank())
                ? safeTitle(extendedPlan.title(), 2) : FALLBACK_TITLES[2];
        results.add(buildResponse(2, extendedTitle, extendedPicked, req.getTravelMode(), req.getDepartureStation(),
                false, extendedDuration, extendedFallbackUsed));

        return results;
    }

    public CourseResponse regenerateCourse(CourseRegenerateRequest req) {
        applyWeatherIfMissing(req);

        List<Place> candidates = selectCandidates(req.getThemeTags());
        if (candidates.size() < 2) {
            candidates = placeMapper.findAllWithLocation();
        }

        int placeCount = req.getCurrentPlaces() != null && !req.getCurrentPlaces().isEmpty()
                ? req.getCurrentPlaces().size()
                : placeCountFor(req.getDurationMinutes());
        int durationMinutes = normalizedDuration(req.getDurationMinutes());
        int usableDuration = usableMinutes(durationMinutes);
        candidates = filterCandidatesByStationRadius(candidates, req.getDepartureStation(), usableDuration, placeCount, req.getTravelMode());

        List<String> lockedPlaceIds = req.getCurrentPlaces() == null
                ? List.of()
                : req.getCurrentPlaces().stream()
                .filter(CourseRegenerateRequest.CurrentPlace::isLocked)
                .map(CourseRegenerateRequest.CurrentPlace::getId)
                .filter(Objects::nonNull)
                .toList();

        List<AiCourseClient.AiCoursePlan> aiPlans =
                aiCourseClient.recommendCourses(req, candidates, placeCount, 1, lockedPlaceIds, usableDuration);
        if (aiPlans.isEmpty()) {
            log.warn("[Course] AI 재추천 결과가 없어 규칙 기반 폴백으로 코스를 재구성합니다. (aiBlocked={})",
                    aiCourseClient.isBlocked());
        }
        List<Place> aiPicked = aiPlans.isEmpty()
                ? List.of()
                : placesByIds(aiPlans.get(0).placeIds(), candidates, placeCount);

        boolean fallbackUsed = false;
        if (!isValidCourse(aiPicked, Math.min(placeCount, candidates.size()), req.getTravelMode(), usableDuration, lockedPlaceIds, req.getDepartureStation())) {
            aiPicked = pickTimeAware(
                    candidates,
                    placeCount,
                    new Random(),
                    req.getTravelMode(),
                    usableDuration,
                    0.70,
                    0.85,
                    lockedPlaceIds,
                    req.getDepartureStation()
            );
            fallbackUsed = true;
        }

        List<Place> merged = mergeLockedPlaces(req, aiPicked, candidates, placeCount);
        merged = trimToFit(merged, req.getTravelMode(), usableDuration, lockedPlaceIds, req.getDepartureStation());
        String title = aiPlans.isEmpty() ? "AI 재추천 코스" : safeTitle(aiPlans.get(0).title(), 0);
        return buildResponse(0, title, merged, req.getTravelMode(), req.getDepartureStation(),
                false, durationMinutes, fallbackUsed || aiPlans.isEmpty());
    }

    public CourseResponse recalculateCourse(CourseRecalculateRequest req) {
        if (req.getPlaceIds() == null || req.getPlaceIds().isEmpty()) {
            return buildResponse(0, req.getTitle() == null ? "편집 코스" : req.getTitle(),
                    List.of(), req.getTravelMode(), req.getDepartureStation(), true,
                    normalizedDuration(req.getDurationMinutes()), false);
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
        return buildResponse(0, title, places, req.getTravelMode(), req.getDepartureStation(), true,
                normalizedDuration(req.getDurationMinutes()), false);
    }

    /**
     * 요청에 날씨가 없으면 출발역 좌표로 조회해 채운다.
     * 조회에 실패해도 UNKNOWN이 들어갈 뿐 코스 추천은 그대로 진행된다.
     */
    private void applyWeatherIfMissing(CourseGenerateRequest req) {
        if (req.getWeatherCondition() != null && !req.getWeatherCondition().isBlank()) {
            return;
        }
        Place station = stationPlace(req.getDepartureStation());
        String condition = weatherService.getCurrentCondition(
                station.getLatitude().doubleValue(), station.getLongitude().doubleValue());
        req.applyWeatherCondition(condition);
    }

    private List<Place> selectCandidates(List<String> themeTags) {
        List<Place> candidates;
        if (themeTags == null || themeTags.isEmpty()) {
            candidates = placeMapper.findAllWithLocation();
        } else {
            List<String> categories = expandThemeTags(themeTags);
            candidates = categories.isEmpty()
                    ? placeMapper.findAllWithLocation()
                    : placeMapper.findByCategoryIn(categories);

            // 선택한 카테고리 안에서 후보가 너무 적으면 코스를 만들 수 없다.
            // 이때만 전체 후보로 넓히고, 넓혔다는 사실을 로그로 남긴다.
            if (candidates.size() < MIN_CANDIDATES_FOR_THEME) {
                log.info("[Course] 카테고리 {} 후보가 {}개뿐이라 전체 장소로 확장합니다.",
                        categories, candidates.size());
                candidates = placeMapper.findAllWithLocation();
            }
        }
        return excludeClosedPlaces(candidates);
    }

    /**
     * 오늘 확실히 휴무인 장소를 후보에서 제외한다.
     * 운영시간 정보가 없는 장소(UNKNOWN)는 남긴다. 정보 부재를 이유로 빼면 후보가 과도하게 줄어든다.
     */
    private List<Place> excludeClosedPlaces(List<Place> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        List<Place> open = candidates.stream()
                .filter(place -> place != null && !place.isDefinitelyClosed())
                .toList();

        int removed = candidates.size() - open.size();
        if (removed > 0) {
            log.info("[Course] 휴무일/영업시간 밖인 장소 {}개를 후보에서 제외했습니다. (남은 후보 {}개)",
                    removed, open.size());
        }
        // 전부 닫혀 있으면(심야 등) 필터를 포기한다. 빈 코스를 주는 것보다는 낫다.
        return open.isEmpty() ? candidates : open;
    }

    /**
     * 테마 태그를 DB 카테고리로 변환한다.
     *
     * <p>과거에는 CAFE를 고르면 FOOD를, NATURE를 고르면 TOUR와 LEPORTS를 함께 넣어
     * "카페를 골랐는데 음식점이 나온다"는 문제가 있었다. 사용자가 고른 카테고리를 그대로 지키고,
     * 후보가 부족한 경우에만 {@link #selectCandidates} 에서 명시적으로 확장한다.
     */
    private List<String> expandThemeTags(List<String> themeTags) {
        Set<String> categories = new LinkedHashSet<>();
        for (String rawTag : themeTags) {
            if (rawTag == null || rawTag.isBlank()) continue;
            categories.add(rawTag.trim().toUpperCase(Locale.ROOT));
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

        // 후보가 부족할 때 예전에는 반경을 통째로 무시하고 전체를 반환했다.
        // 그 결과 카테고리를 좁힐수록 오히려 추천 범위가 넓어지는 역효과가 있었다.
        // 이제는 상한 반경까지만 완화하고, 완화 사실을 로그로 남긴다.
        double relaxedRadiusKm = radiusKm * RADIUS_RELAX_MULTIPLIER;
        List<Place> relaxed = sortedByStationDistance.stream()
                .filter(place -> distanceFromStation(station, place) <= relaxedRadiusKm)
                .toList();
        log.info("[Course] 반경 {}km 후보가 {}개뿐이라 {}km까지 완화했습니다. (완화 후 {}개)",
                String.format("%.1f", radiusKm), withinRadius.size(),
                String.format("%.1f", relaxedRadiusKm), relaxed.size());

        if (!relaxed.isEmpty()) {
            return relaxed;
        }
        // 상한 반경 안에도 아무것도 없으면 가장 가까운 곳들이라도 쓴다.
        return sortedByStationDistance.isEmpty()
                ? candidates
                : sortedByStationDistance.subList(0, Math.min(minimumNeeded, sortedByStationDistance.size()));
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

    /**
     * 출발역 기준 후보 반경.
     *
     * <p>환승 관광은 "역 근처에서 짧게"가 핵심이다. 예전 값(택시 최대 12km)은 잔여 시간이 길면
     * 대전 전역이 후보가 되어 추천이 산만해졌다. 이동 시간이 늘수록 체류 시간이 줄어들기 때문에
     * 시간이 많다고 반경을 비례해서 늘리는 것은 오히려 코스 품질을 떨어뜨린다.
     */
    private double radiusKmFor(int durationMinutes, boolean walkOnly) {
        if (walkOnly) {
            if (durationMinutes <= 180) return 1.5;
            if (durationMinutes <= 300) return 2.5;
            return 3.5;
        }

        if (durationMinutes <= 180) return 3.0;
        if (durationMinutes <= 300) return 5.0;
        return 8.0;
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
                                                  boolean calculateAllRouteModes) {
        String key = travelMode + ":" + calculateAllRouteModes + ":" + from.getId() + "->" + to.getId();
        return cache.computeIfAbsent(key, ignored -> calcTransport(from, to, travelMode, calculateAllRouteModes));
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

    /**
     * 코스 편성에 실제로 쓸 수 있는 시간. 잔여 시간에서 복귀 버퍼를 뺀 값이다.
     * 버퍼가 잔여 시간보다 큰 짧은 환승에서도 최소 시간은 남긴다.
     */
    private int usableMinutes(int durationMinutes) {
        int buffer = Math.max(0, returnBufferMinutes);
        return Math.max(MIN_USABLE_MINUTES, durationMinutes - buffer);
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
        return buildResponse(index, title, places, travelMode, departureStation, false, 0, false);
    }

    private CourseResponse buildResponse(int index, String title, List<Place> places, String travelMode,
                                         String departureStation, boolean calculateAllRouteModes) {
        return buildResponse(index, title, places, travelMode, departureStation, calculateAllRouteModes, 0, false);
    }

    private CourseResponse buildResponse(int index, String title, List<Place> places, String travelMode,
                                         String departureStation, boolean calculateAllRouteModes,
                                         int timeBudgetMinutes, boolean fallbackUsed) {
        List<CourseStopResponse> stops = new ArrayList<>();
        int totalMinutes = 0;
        int totalFare = 0;
        Map<String, TransportInfoResponse> transportCache = new HashMap<>();
        Place station = stationPlace(departureStation);

        TransportInfoResponse departureTransport = null;
        TransportInfoResponse returnTransport = null;
        if (!places.isEmpty()) {
            departureTransport = cachedTransport(station, places.get(0), travelMode, transportCache, calculateAllRouteModes);
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
                transport = cachedTransport(cur, places.get(i + 1), travelMode, transportCache, calculateAllRouteModes);
                boolean isWalk = "WALK".equals(travelMode);
                totalMinutes += parseMin(isWalk ? transport.getWalkTime() : transport.getTaxiTime());
                if (!isWalk) totalFare += transport.getTaxiFare();
            }
            if (i == places.size() - 1) {
                returnTransport = cachedTransport(cur, station, travelMode, transportCache, calculateAllRouteModes);
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
                stops,
                recommendationReason(index, places, travelMode, departureStation, timeBudgetMinutes, totalMinutes, fallbackUsed),
                timeBudgetMinutes,
                totalMinutes,
                returnBufferMinutes,
                dataSources(calculateAllRouteModes),
                fallbackUsed
        );
    }

    private String recommendationReason(int index, List<Place> places, String travelMode, String departureStation,
                                        int timeBudgetMinutes, int totalMinutes, boolean fallbackUsed) {
        String stationName = stationPlace(departureStation).getName();
        String modeLabel = "WALK".equals(travelMode) ? "도보" : "택시";
        String basis = fallbackUsed ? "AI 응답 검증 후 시간/카테고리 규칙 기반으로 보정" : "AI 후보 검증과 시간 예산 계산";
        int realPlaceCount = places == null ? 0 : places.size();
        String budgetText = timeBudgetMinutes > 0
                ? String.format("사용 가능 %d분 중 약 %d분", timeBudgetMinutes, totalMinutes)
                : String.format("약 %d분", totalMinutes);
        return String.format("%s 기준 %s 이동으로 %d곳을 연결했습니다. %s을 반영했고, %s 소요로 계산했습니다.",
                stationName, modeLabel, realPlaceCount, basis, budgetText);
    }

    private List<String> dataSources(boolean calculateAllRouteModes) {
        List<String> sources = new ArrayList<>();
        sources.add("한국관광공사 TourAPI");
        sources.add("카카오 지도/모빌리티 경로");
        if (calculateAllRouteModes) {
            sources.add("사용자 편집 코스 재계산");
        }
        return sources;
    }

    private TransportInfoResponse calcTransport(Place from, Place to, String travelMode) {
        return calcTransport(from, to, travelMode, false);
    }

    private TransportInfoResponse calcTransport(Place from, Place to, String travelMode, boolean calculateAllRouteModes) {
        if (from.getLatitude() == null || from.getLongitude() == null
                || to.getLatitude() == null || to.getLongitude() == null) {
            return new TransportInfoResponse("정보 없음", "정보 없음", "정보 없음", 0, List.of(),
                    "UNAVAILABLE", "UNAVAILABLE", "UNAVAILABLE", "UNAVAILABLE");
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

        List<double[]> routePath = List.of();
        String walkSource = "ESTIMATED";
        String taxiSource = "ESTIMATED";
        String busSource = "UNAVAILABLE";
        String routePathSource = "STRAIGHT_LINE";

        if (calculateAllRouteModes || "WALK".equals(travelMode)) {
            KakaoRouteApiClient.WalkRouteResult walkResult = kakaoRouteApiClient.getWalkRouteResult(fLat, fLng, tLat, tLng);
            if (walkResult.minutes() > 0) {
                walkMin = walkResult.minutes();
                walkSource = "KAKAO";
            }
            if (!walkResult.path().isEmpty()) {
                routePath = walkResult.path();
                routePathSource = "KAKAO";
            }
        }
        if (calculateAllRouteModes || !"WALK".equals(travelMode)) {
            KakaoRouteApiClient.CarRouteResult carResult = kakaoRouteApiClient.getCarRouteResult(fLat, fLng, tLat, tLng);
            if (carResult.minutes() > 0) {
                taxiMin = carResult.minutes();
                taxiSource = "KAKAO_MOBILITY";
            }
            if (carResult.taxiFare() > 0) {
                fare = carResult.taxiFare();
                taxiSource = "KAKAO_MOBILITY";
            }
            if (routePath.isEmpty() && !carResult.path().isEmpty()) {
                routePath = carResult.path();
                routePathSource = "KAKAO_MOBILITY";
            }
        }

        KakaoRouteApiClient.PublicTransitRouteResult transitResult =
                kakaoRouteApiClient.getPublicTransitRouteResult(fLat, fLng, tLat, tLng);
        int busMin;
        if (transitResult.minutes() > 0) {
            busMin = transitResult.minutes();
            busSource = "KAKAO";
        } else {
            busMin = busService.estimateBusMinutes(fLat, fLng, tLat, tLng);
            if (busMin > 0) {
                busSource = "BUS_STOP_ESTIMATE";
            }
        }

        String busTime = busMin > 0 ? busMin + "분" : "정보 없음";
        return new TransportInfoResponse(walkMin + "분", busTime, taxiMin + "분", fare, routePath,
                walkSource, busSource, taxiSource, routePathSource);
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
    
    private String safeTitle(String aiTitle, int index) {
        if (aiTitle == null || aiTitle.isBlank()) return FALLBACK_TITLES[Math.min(index, FALLBACK_TITLES.length - 1)];
        boolean hasKorean = aiTitle.chars().anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3);
        return hasKorean ? aiTitle : FALLBACK_TITLES[Math.min(index, FALLBACK_TITLES.length - 1)];
    }
}
