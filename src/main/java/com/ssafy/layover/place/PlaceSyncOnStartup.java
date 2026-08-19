package com.ssafy.layover.place;

import com.ssafy.layover.place.dto.PlaceSyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "place.sync.on-startup", havingValue = "true")
public class PlaceSyncOnStartup {

    private final PlaceMapper placeMapper;
    private final TourApiService tourApiService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncWhenDatabaseIsEmpty() {
        int existingTourApiPlaces = placeMapper.countTourApiPlaces();
        if (existingTourApiPlaces > 0) {
            log.info("[PlaceSyncOnStartup] 기존 관광지 {}개가 있어 자동 동기화를 건너뜁니다.", existingTourApiPlaces);
            return;
        }

        log.info("[PlaceSyncOnStartup] 관광지 데이터가 없어 최초 동기화를 시작합니다.");
        try {
            PlaceSyncResult result = tourApiService.syncPlaces();
            log.info(
                    "[PlaceSyncOnStartup] 최초 동기화 완료: 저장={}, 실패={}",
                    result.getSavedCount(),
                    result.getErrorCount()
            );
        } catch (Exception e) {
            // 외부 API 장애가 서버 자체의 기동을 막아서는 안 된다.
            log.error("[PlaceSyncOnStartup] 최초 동기화 실패. 관리자 API로 다시 시도할 수 있습니다.", e);
        }
    }
}
