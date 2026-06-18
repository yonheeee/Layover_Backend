package com.ssafy.layover.bus;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusApiClient {

    private static final String BASE_URL = "https://apis.data.go.kr/6300000/busRouteInfo/getStaionByRouteAll";
    private static final int MAX_PAGES = 20;

    private final RestTemplate restTemplate;

    @Value("${daejeon.bus.api.key:}")
    private String serviceKey;

    // busStopId 기준 중복 제거
    private final Map<String, BusStop> stopCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadStops() {
        if (serviceKey == null || serviceKey.isBlank()) {
            log.warn("대전 버스 API 키가 설정되지 않아 버스 정류소를 불러오지 않습니다.");
            return;
        }
        try {
            for (int page = 1; page <= MAX_PAGES; page++) {
                List<BusStop> stops = fetchPage(page);
                if (stops.isEmpty()) break;
                stops.forEach(s -> stopCache.put(s.busStopId(), s));
                log.info("버스 정류소 로드 중 — {}페이지 ({} 누적)", page, stopCache.size());
            }
            log.info("버스 정류소 캐시 완료 — 총 {}개", stopCache.size());
        } catch (Exception e) {
            log.error("버스 정류소 로드 실패: {}", e.getMessage());
        }
    }

    public Collection<BusStop> getAllStops() {
        return stopCache.values();
    }

    private List<BusStop> fetchPage(int page) throws Exception {
        String encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
        String url = BASE_URL + "?serviceKey=" + encodedKey + "&reqPage=" + page;
        log.info("버스 API 요청 URL: {}", url);
        String xml = restTemplate.getForObject(url, String.class);
        if (xml == null || xml.isBlank()) return List.of();
        return parseStops(xml);
    }

    private List<BusStop> parseStops(String xml) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        NodeList items = doc.getElementsByTagName("itemList");
        List<BusStop> result = new ArrayList<>();

        for (int i = 0; i < items.getLength(); i++) {
            Element el = (Element) items.item(i);
            String id  = text(el, "BUS_STOP_ID");
            String nm  = text(el, "BUSSTOP_NM");
            String lat = text(el, "GPS_LATI");
            String lng = text(el, "GPS_LONG");

            if (id.isBlank() || lat.isBlank() || lng.isBlank()) continue;
            try {
                result.add(new BusStop(id, nm, Double.parseDouble(lat), Double.parseDouble(lng)));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private String text(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        return nl.getLength() > 0 ? nl.item(0).getTextContent().trim() : "";
    }
}
