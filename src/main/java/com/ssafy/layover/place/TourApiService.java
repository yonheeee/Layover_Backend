package com.ssafy.layover.place;

import com.ssafy.layover.place.dto.PlaceSyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiService {

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2";

    private static final Map<Integer, String> TYPE_CATEGORY_MAP = createTypeCategoryMap();

    private static Map<Integer, String> createTypeCategoryMap() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(12, "TOUR");
        map.put(14, "CULTURE");
        map.put(15, "FESTIVAL");
        map.put(28, "LEPORTS");
        map.put(32, "STAY");
        map.put(38, "SHOPPING");
        map.put(39, "FOOD");
        return Collections.unmodifiableMap(map);
    }

    @Value("${tour.api.key}")
    private String serviceKey;
    
    private final RestTemplate restTemplate;
    private final PlaceMapper placeMapper;
    private final KakaoLocalApiClient kakaoLocalApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlaceSyncResult syncPlaces() {
        int savedCount = 0;
        int errorCount = 0;

        for (Map.Entry<Integer, String> entry : TYPE_CATEGORY_MAP.entrySet()) {
            int contentTypeId = entry.getKey();
            String category = entry.getValue();

            try {
                List<Map<String, Object>> items = fetchAreaBasedList(contentTypeId);
                log.info("[TourAPI] contentTypeId={} 목록 {}건", contentTypeId, items.size());

                for (Map<String, Object> item : items) {
                    try {
                        String contentId = strVal(item, "contentid");
                        if (contentId.isBlank()) continue;

                        Place place = buildPlace(item, contentTypeId, category, contentId);
                        placeMapper.upsertPlace(place);
                        savedCount++;

                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    } catch (Exception e) {
                        log.warn("[TourAPI] 처리 실패 contentId={}: {}", strVal(item, "contentid"), e.getMessage());
                        errorCount++;
                    }
                }
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                log.error("[TourAPI] contentTypeId={} 전체 실패: {}", contentTypeId, e.getMessage());
                errorCount++;
            }
        }

        log.info("[TourAPI] 동기화 완료 — 저장: {}, 실패: {}", savedCount, errorCount);
        return PlaceSyncResult.of(savedCount, errorCount);
    }

    private Place buildPlace(Map<String, Object> item, int contentTypeId, String category, String contentId) {
        Place place = new Place();
        place.setName(strVal(item, "title"));
        place.setCategory(category);
        place.setOriginalCategoryCode(String.valueOf(contentTypeId));
        place.setContentTypeId(String.valueOf(contentTypeId));
        place.setTourApiId(contentId);
        place.setAddress(strVal(item, "addr1"));
        place.setImageUrl(strVal(item, "firstimage"));

        String mapx = strVal(item, "mapx");
        String mapy = strVal(item, "mapy");
        if (!mapx.isBlank()) place.setLongitude(new BigDecimal(mapx));
        if (!mapy.isBlank()) place.setLatitude(new BigDecimal(mapy));

        place.setDescription(fetchDescription(contentId));
        applyIntroDetails(place, contentId, contentTypeId);
        place.setDetailInfoRaw(fetchDetailInfoRaw(contentId, contentTypeId));
        applyKakaoLocalDetails(place);

        String imgUrl = fetchImage(contentId);
        if (!imgUrl.isBlank()) place.setImageUrl(imgUrl);

        return place;
    }

    private List<Map<String, Object>> fetchAreaBasedList(int contentTypeId) {
    	String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
    	String url = BASE_URL + "/areaBasedList2?serviceKey=" + encodedKey
                + "&MobileOS=ETC&MobileApp=Layover&_type=json"
                + "&areaCode=3&contentTypeId=" + contentTypeId
                + "&numOfRows=1000&pageNo=1";
        return extractItems(getAsMap(url));
    }

    private String fetchDescription(String contentId) {
        try {
        	String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
        	String url = BASE_URL + "/detailCommon2?serviceKey=" + encodedKey
                    + "&MobileOS=ETC&MobileApp=Layover&_type=json"
                    + "&contentId=" + contentId;
            List<Map<String, Object>> items = extractItems(getAsMap(url));
            return items.isEmpty() ? "" : strVal(items.get(0), "overview");
        } catch (Exception e) {
            return "";
        }
    }

    private void applyIntroDetails(Place place, String contentId, int contentTypeId) {
        try {
        	String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
        	String url = BASE_URL + "/detailIntro2?serviceKey=" + encodedKey
                    + "&MobileOS=ETC&MobileApp=Layover&_type=json"
                    + "&contentId=" + contentId + "&contentTypeId=" + contentTypeId;
            List<Map<String, Object>> items = extractItems(getAsMap(url));
            if (items.isEmpty()) return;

            Map<String, Object> intro = items.get(0);
            place.setOperatingHours(extractOperatingHours(intro, contentTypeId));
            place.setRestDate(extractRestDate(intro, contentTypeId));
            place.setInfoCenter(extractInfoCenter(intro, contentTypeId));
            place.setParking(extractParking(intro, contentTypeId));
            place.setUseFee(extractUseFee(intro, contentTypeId));
            place.setReservation(extractReservation(intro, contentTypeId));
            place.setIntroRaw(toJson(intro));
        } catch (Exception e) {
            log.debug("[TourAPI] detailIntro failed contentId={}: {}", contentId, e.getMessage());
        }
    }

    private String fetchDetailInfoRaw(String contentId, int contentTypeId) {
        try {
            String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
            String url = BASE_URL + "/detailInfo2?serviceKey=" + encodedKey
                    + "&MobileOS=ETC&MobileApp=Layover&_type=json"
                    + "&contentId=" + contentId + "&contentTypeId=" + contentTypeId;
            List<Map<String, Object>> items = extractItems(getAsMap(url));
            return items.isEmpty() ? "" : toJson(items);
        } catch (Exception e) {
            log.debug("[TourAPI] detailInfo failed contentId={}: {}", contentId, e.getMessage());
            return "";
        }
    }

    private String fetchImage(String contentId) {
        try {
        	String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
        	String url = BASE_URL + "/detailImage2?serviceKey=" + encodedKey
                    + "&MobileOS=ETC&MobileApp=Layover&_type=json"
                    + "&contentId=" + contentId + "&imageYN=Y";
            List<Map<String, Object>> items = extractItems(getAsMap(url));
            return items.isEmpty() ? "" : strVal(items.get(0), "originimgurl");
        } catch (Exception e) {
            return "";
        }
    }

    private String extractOperatingHours(Map<String, Object> item, int contentTypeId) {
        return switch (contentTypeId) {
            case 12 -> strVal(item, "usetime");
            case 14 -> strVal(item, "usetimeculture");
            case 15 -> strVal(item, "eventstartdate") + "~" + strVal(item, "eventenddate");
            case 28 -> strVal(item, "usetimeleports");
            case 32 -> "체크인 " + strVal(item, "checkintime") + " / 체크아웃 " + strVal(item, "checkouttime");
            case 38 -> strVal(item, "opentime");
            case 39 -> strVal(item, "opentimefood");
            default -> "";
        };
    }

    private String extractRestDate(Map<String, Object> item, int contentTypeId) {
        return switch (contentTypeId) {
            case 12 -> strVal(item, "restdate");
            case 14 -> strVal(item, "restdateculture");
            case 28 -> strVal(item, "restdateleports");
            case 38 -> strVal(item, "restdateshopping");
            case 39 -> strVal(item, "restdatefood");
            default -> "";
        };
    }

    private String extractInfoCenter(Map<String, Object> item, int contentTypeId) {
        return switch (contentTypeId) {
            case 12 -> strVal(item, "infocenter");
            case 14 -> strVal(item, "infocenterculture");
            case 15 -> strVal(item, "sponsor1tel").isBlank() ? strVal(item, "sponsor2tel") : strVal(item, "sponsor1tel");
            case 28 -> strVal(item, "infocenterleports");
            case 38 -> strVal(item, "infocentershopping");
            case 39 -> strVal(item, "infocenterfood");
            default -> "";
        };
    }

    private String extractParking(Map<String, Object> item, int contentTypeId) {
        return switch (contentTypeId) {
            case 12 -> strVal(item, "parking");
            case 14 -> strVal(item, "parkingculture");
            case 28 -> strVal(item, "parkingleports");
            case 32 -> strVal(item, "parkinglodging");
            case 38 -> strVal(item, "parkingshopping");
            case 39 -> strVal(item, "parkingfood");
            default -> "";
        };
    }

    private String extractUseFee(Map<String, Object> item, int contentTypeId) {
        return switch (contentTypeId) {
            case 12 -> strVal(item, "usefee");
            case 14 -> strVal(item, "usefee");
            case 15 -> strVal(item, "usetimefestival");
            case 28 -> strVal(item, "usefeeleports");
            case 39 -> firstNonBlank(strVal(item, "firstmenu"), strVal(item, "treatmenu"));
            default -> "";
        };
    }

    private String extractReservation(Map<String, Object> item, int contentTypeId) {
        return switch (contentTypeId) {
            case 12 -> strVal(item, "expguide");
            case 28 -> strVal(item, "reservation");
            case 32 -> strVal(item, "reservationlodging");
            default -> "";
        };
    }

    private void applyKakaoLocalDetails(Place place) {
        Double lat = place.getLatitude() != null ? place.getLatitude().doubleValue() : null;
        Double lng = place.getLongitude() != null ? place.getLongitude().doubleValue() : null;
        kakaoLocalApiClient.findBestMatch(place.getName(), lat, lng).ifPresent(match -> {
            place.setKakaoPlaceId(match.id());
            place.setKakaoPlaceUrl(match.placeUrl());
            place.setKakaoPhone(match.phone());
            place.setRoadAddress(match.roadAddress());
        });
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "";
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<String, Object> getAsMap(String url) {
        Map raw = restTemplate.getForObject(url, Map.class);
        return raw != null ? (Map<String, Object>) raw : Map.of();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Map<String, Object>> extractItems(Map<String, Object> root) {
        try {
            Map response = (Map) root.get("response");
            if (response == null) return List.of();
            Map body = (Map) response.get("body");
            if (body == null) return List.of();
            Object items = body.get("items");
            if (!(items instanceof Map)) return List.of();
            Object item = ((Map) items).get("item");
            if (item == null) return List.of();
            if (item instanceof List) return (List<Map<String, Object>>) item;
            if (item instanceof Map) return List.of((Map<String, Object>) item);
            return List.of();
        } catch (Exception e) {
            log.warn("[TourAPI] 응답 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private String strVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : "";
    }
}
