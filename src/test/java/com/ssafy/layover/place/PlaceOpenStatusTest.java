package com.ssafy.layover.place;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TourAPI 운영시간/휴무일 텍스트 해석 테스트.
 *
 * <p>실제 TourAPI 응답은 형식이 제각각이라, 여기 모아둔 표기들이 회귀하지 않는지 확인한다.
 */
class PlaceOpenStatusTest {

    // 2026-08-17(월) / 2026-08-18(화)
    private static final LocalDateTime MON_15 = LocalDateTime.of(2026, 8, 17, 15, 0);
    private static final LocalDateTime TUE_15 = LocalDateTime.of(2026, 8, 18, 15, 0);
    private static final LocalDateTime TUE_07 = LocalDateTime.of(2026, 8, 18, 7, 0);
    private static final LocalDateTime TUE_23_30 = LocalDateTime.of(2026, 8, 18, 23, 30);

    @Nested
    @DisplayName("휴무일")
    class RestDate {

        @Test
        @DisplayName("매주 월요일 휴관이면 월요일에는 CLOSED")
        void closedOnRestDay() {
            assertThat(PlaceOpenStatus.evaluate("09:00~18:00", "매주 월요일", MON_15))
                    .isEqualTo(PlaceOpenStatus.Status.CLOSED);
        }

        @Test
        @DisplayName("매주 월요일 휴관이어도 화요일 영업시간 안이면 OPEN")
        void openOnOtherDay() {
            assertThat(PlaceOpenStatus.evaluate("09:00~18:00", "매주 월요일", TUE_15))
                    .isEqualTo(PlaceOpenStatus.Status.OPEN);
        }

        @Test
        @DisplayName("쉼표로 나열된 휴무일의 마지막 요일도 인식한다")
        void closedOnCommaSeparatedRestDay() {
            assertThat(PlaceOpenStatus.evaluate("09:00~18:00", "월,화 휴무", TUE_15))
                    .isEqualTo(PlaceOpenStatus.Status.CLOSED);
        }

        @Test
        @DisplayName("연중무휴면 요일 판정을 하지 않는다")
        void noRestDay() {
            assertThat(PlaceOpenStatus.evaluate("10:00 - 22:00", "연중무휴", TUE_15))
                    .isEqualTo(PlaceOpenStatus.Status.OPEN);
        }

        @Test
        @DisplayName("휴무일이 '없음'이면 열려 있는 것으로 본다")
        void restDateNone() {
            assertThat(PlaceOpenStatus.evaluate("09:00~18:00", "없음", MON_15))
                    .isEqualTo(PlaceOpenStatus.Status.OPEN);
        }

        @Test
        @DisplayName("휴무를 뜻하지 않는 문장의 요일 글자에 오탐하지 않는다")
        void doesNotMisreadProse() {
            // '수'가 요일이 아니라 의존명사로 쓰인 경우 (수요일에 검사)
            LocalDateTime wednesday = LocalDateTime.of(2026, 8, 19, 15, 0);
            assertThat(PlaceOpenStatus.evaluate("09:00~18:00", "연중 이용할 수 있습니다", wednesday))
                    .isEqualTo(PlaceOpenStatus.Status.OPEN);
        }
    }

    @Nested
    @DisplayName("운영시간")
    class OperatingHours {

        @Test
        @DisplayName("영업 시작 전이면 CLOSED")
        void beforeOpening() {
            assertThat(PlaceOpenStatus.evaluate("09:00~18:00", null, TUE_07))
                    .isEqualTo(PlaceOpenStatus.Status.CLOSED);
        }

        @Test
        @DisplayName("한글 오전/오후 표기를 해석한다")
        void koreanMeridiem() {
            assertThat(PlaceOpenStatus.evaluate("오전 9시 ~ 오후 6시", null, TUE_15))
                    .isEqualTo(PlaceOpenStatus.Status.OPEN);
        }

        @Test
        @DisplayName("자정을 넘기는 영업시간을 처리한다")
        void overnight() {
            assertThat(PlaceOpenStatus.evaluate("18:00~02:00", null, TUE_23_30))
                    .isEqualTo(PlaceOpenStatus.Status.OPEN);
            assertThat(PlaceOpenStatus.evaluate("18:00~02:00", null, TUE_15))
                    .isEqualTo(PlaceOpenStatus.Status.CLOSED);
        }

        @Test
        @DisplayName("HTML 태그가 섞여 있어도 해석한다")
        void stripsHtml() {
            assertThat(PlaceOpenStatus.evaluate("<p>09:00~18:00</p>", null, TUE_15))
                    .isEqualTo(PlaceOpenStatus.Status.OPEN);
        }
    }

    @Nested
    @DisplayName("정보 부족")
    class Unknown {

        @Test
        @DisplayName("운영시간 정보가 없으면 UNKNOWN")
        void noOperatingHours() {
            assertThat(PlaceOpenStatus.evaluate(null, "연중무휴", TUE_15))
                    .isEqualTo(PlaceOpenStatus.Status.UNKNOWN);
        }

        @Test
        @DisplayName("아무 정보도 없으면 UNKNOWN")
        void nothing() {
            assertThat(PlaceOpenStatus.evaluate(null, null, TUE_15))
                    .isEqualTo(PlaceOpenStatus.Status.UNKNOWN);
        }

        @Test
        @DisplayName("시간을 해석할 수 없으면 UNKNOWN. 열려 있다고 단정하지 않는다")
        void unparseableHours() {
            assertThat(PlaceOpenStatus.evaluate("상시 개방", null, TUE_23_30))
                    .isEqualTo(PlaceOpenStatus.Status.UNKNOWN);
        }
    }
}
