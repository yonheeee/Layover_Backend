package com.ssafy.layover.bus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BusService {

    private final BusApiClient busApiClient;

    // 두 지점 사이 버스 이동시간 추정 (분)
    // 가장 가까운 버스 정류소까지 도보 + 버스 대기 + 버스 이동 + 도착지 도보
    public int estimateBusMinutes(double fromLat, double fromLng, double toLat, double toLng) {
        Optional<BusStop> fromStop = findNearestStop(fromLat, fromLng);
        Optional<BusStop> toStop   = findNearestStop(toLat, toLng);

        if (fromStop.isEmpty() || toStop.isEmpty()) {
            // 정류소 데이터 없으면 직선거리 기반 추정
            double dist = haversine(fromLat, fromLng, toLat, toLng);
            return Math.max(5, (int) Math.round(dist / 20.0 * 60) + 5);
        }

        double walkToStop    = haversine(fromLat, fromLng, fromStop.get().lat(), fromStop.get().lng());
        double busDist       = haversine(fromStop.get().lat(), fromStop.get().lng(),
                                         toStop.get().lat(), toStop.get().lng());
        double walkFromStop  = haversine(toStop.get().lat(), toStop.get().lng(), toLat, toLng);

        int walkToMin   = (int) Math.round(walkToStop / 4.0 * 60);
        int waitMin     = 5; // 평균 배차 대기
        int busMin      = Math.max(3, (int) Math.round(busDist / 20.0 * 60));
        int walkFromMin = (int) Math.round(walkFromStop / 4.0 * 60);

        return walkToMin + waitMin + busMin + walkFromMin;
    }

    public List<BusStop> getAllStops() {
        return List.copyOf(busApiClient.getAllStops());
    }

    private Optional<BusStop> findNearestStop(double lat, double lng) {
        return busApiClient.getAllStops().stream()
                .min(Comparator.comparingDouble(s -> haversine(lat, lng, s.lat(), s.lng())));
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
}
