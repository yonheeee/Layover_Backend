package com.ssafy.layover.weather;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기상청이 공개한 격자표의 대표 지점으로 변환식을 검증한다.
 */
class KmaGridConverterTest {

    @Test
    @DisplayName("주요 도시 대표 지점이 공식 격자 번호와 일치한다")
    void matchesOfficialGrid() {
        assertThat(KmaGridConverter.toGrid(37.5665, 126.9780))
                .isEqualTo(new KmaGridConverter.Grid(60, 127));   // 서울시청
        assertThat(KmaGridConverter.toGrid(36.3504, 127.3845))
                .isEqualTo(new KmaGridConverter.Grid(67, 100));   // 대전시청
        assertThat(KmaGridConverter.toGrid(35.1796, 129.0756))
                .isEqualTo(new KmaGridConverter.Grid(98, 76));    // 부산시청
        assertThat(KmaGridConverter.toGrid(35.1595, 126.8526))
                .isEqualTo(new KmaGridConverter.Grid(58, 74));    // 광주시청
        assertThat(KmaGridConverter.toGrid(37.4563, 126.7052))
                .isEqualTo(new KmaGridConverter.Grid(55, 124));   // 인천시청
    }

    @Test
    @DisplayName("서비스가 사용하는 세 출발역이 대전 인근 격자로 변환된다")
    void departureStations() {
        // 격자는 5km 단위라 대전역과 대전시청이 인접 격자로 갈라질 수 있다.
        assertThat(KmaGridConverter.toGrid(36.3325, 127.4348).nx()).isBetween(67, 68);
        assertThat(KmaGridConverter.toGrid(36.3226, 127.4039).nx()).isBetween(67, 68);
        assertThat(KmaGridConverter.toGrid(36.4518, 127.4297).ny()).isBetween(100, 104);
    }
}
