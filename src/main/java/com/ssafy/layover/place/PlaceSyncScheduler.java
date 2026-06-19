package com.ssafy.layover.place;

import com.ssafy.layover.place.dto.PlaceSyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceSyncScheduler {

    private final TourApiService tourApiService;

    @Scheduled(cron = "0 0 2 1 * *")
    public void syncPlaces() {
        log.info("[PlaceSyncScheduler] 관광지 자동 동기화 시작");
        PlaceSyncResult result = tourApiService.syncPlaces();
        log.info("[PlaceSyncScheduler] 완료 — 저장: {}, 실패: {}", result.getSavedCount(), result.getErrorCount());
    }
}
