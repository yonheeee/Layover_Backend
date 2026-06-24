package com.ssafy.layover.place;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationPlaceSeeder {

    public static final String DAEJEON_STATION_ID = "STATION_DAEJEON";
    public static final String SEODAEJEON_STATION_ID = "STATION_SEODAEJEON";
    public static final String SINTANJIN_STATION_ID = "STATION_SINTANJIN";

    private final PlaceMapper placeMapper;

    @PostConstruct
    public void seedOnStartup() {
        upsertStations();
    }

    public void upsertStations() {
        List<Place> stations = List.of(
                station(DAEJEON_STATION_ID, "대전역", "대전광역시 동구 중앙로 215", 36.3325, 127.4348),
                station(SEODAEJEON_STATION_ID, "서대전역", "대전광역시 중구 오류로 23", 36.3226, 127.4039),
                station(SINTANJIN_STATION_ID, "신탄진역", "대전광역시 대덕구 신탄진로 807", 36.4518, 127.4297)
        );

        for (Place station : stations) {
            placeMapper.upsertStationPlace(station);
        }
        log.info("[StationPlaceSeeder] 역 기준 장소 upsert 완료: {}개", stations.size());
    }

    private Place station(String id, String name, String address, double lat, double lng) {
        Place place = new Place();
        place.setId(id);
        place.setName(name);
        place.setCategory("STATION");
        place.setOriginalCategoryCode("STATION");
        place.setContentTypeId("STATION");
        place.setTourApiId(id);
        place.setAddress(address);
        place.setLatitude(BigDecimal.valueOf(lat));
        place.setLongitude(BigDecimal.valueOf(lng));
        place.setOperatingHours("상시 이용");
        place.setDescription("Layover 서비스에서 출발지와 도착지 기준점으로 사용하는 철도역입니다.");
        place.setImageUrl("");
        place.setActive(true);
        return place;
    }
}
