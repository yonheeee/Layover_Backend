package com.ssafy.layover.bus;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bus")
@RequiredArgsConstructor
public class BusController {

    private final BusService busService;

    // 지도에 버스 정류소 마커를 표시하기 위한 전체 정류소 목록
    @GetMapping("/stops")
    public ResponseEntity<List<BusStopResponse>> getStops() {
        List<BusStopResponse> stops = busService.getAllStops()
                .stream()
                .map(BusStopResponse::from)
                .toList();
        return ResponseEntity.ok(stops);
    }
}
