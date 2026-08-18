package com.ssafy.layover.stamp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스탬프 위치 검증에 쓰는 거리 계산 테스트.
 * 프론트엔드(StampTourView)의 haversine과 같은 결과를 내야 한다.
 */
class StampServiceDistanceTest {

    private static final double DAEJEON_STATION_LAT = 36.3325;
    private static final double DAEJEON_STATION_LNG = 127.4348;

    @Test
    @DisplayName("같은 지점이면 0m")
    void samePoint() {
        double distance = StampService.distanceMeters(
                DAEJEON_STATION_LAT, DAEJEON_STATION_LNG,
                DAEJEON_STATION_LAT, DAEJEON_STATION_LNG);
        assertThat(distance).isZero();
    }

    @Test
    @DisplayName("위도 0.001도 차이는 약 111m")
    void shortDistance() {
        double distance = StampService.distanceMeters(
                DAEJEON_STATION_LAT, DAEJEON_STATION_LNG,
                DAEJEON_STATION_LAT + 0.001, DAEJEON_STATION_LNG);
        assertThat(distance).isBetween(105.0, 118.0);
    }

    @Test
    @DisplayName("대전역과 서대전역은 1km보다 멀다")
    void betweenStations() {
        double distance = StampService.distanceMeters(
                DAEJEON_STATION_LAT, DAEJEON_STATION_LNG,
                36.3226, 127.4039);
        assertThat(distance).isGreaterThan(1000.0);
    }

    @Test
    @DisplayName("검증 반경 100m 안팎을 구분한다")
    void aroundVerificationRadius() {
        double inside = StampService.distanceMeters(
                DAEJEON_STATION_LAT, DAEJEON_STATION_LNG,
                DAEJEON_STATION_LAT + 0.0005, DAEJEON_STATION_LNG);
        double outside = StampService.distanceMeters(
                DAEJEON_STATION_LAT, DAEJEON_STATION_LNG,
                DAEJEON_STATION_LAT + 0.005, DAEJEON_STATION_LNG);

        assertThat(inside).isLessThan(100.0);
        assertThat(outside).isGreaterThan(100.0);
    }
}
