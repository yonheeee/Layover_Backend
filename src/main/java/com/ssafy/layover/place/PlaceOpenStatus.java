package com.ssafy.layover.place;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TourAPI가 내려주는 자유 형식 텍스트(운영시간, 휴무일)로 현재 영업 여부를 판정한다.
 *
 * <p>과거에는 {@code hour >= 9 && hour < 21} 로 고정되어 있어, 월요일 휴관인 박물관도
 * 오후 3시에는 "영업중"으로 표시됐다. "지금 갈 수 있는 곳"을 고르는 것이 서비스의 전제이므로
 * 실제 데이터를 반영하되, <b>모르는 것은 모른다고</b> 답하도록 {@link Status#UNKNOWN} 을 둔다.
 *
 * <p>TourAPI 텍스트는 형식이 제각각이라 완전한 파싱은 불가능하다. 따라서 판정 원칙은
 * "확실할 때만 CLOSED"이다. 애매하면 UNKNOWN으로 두고 사용자가 직접 확인하도록 안내한다.
 * 잘못 열려 있다고 하는 것보다 잘못 닫혀 있다고 하는 쪽이 사용자에게 더 큰 손해이기 때문이다.
 */
public final class PlaceOpenStatus {

    public enum Status {
        /** 운영시간 정보가 있고, 현재 영업 중 */
        OPEN,
        /** 휴무일이거나 운영시간 밖 */
        CLOSED,
        /** 정보가 없거나 해석할 수 없음 */
        UNKNOWN
    }

    /** "연중무휴", "연중개방" 처럼 휴무일이 없음을 뜻하는 표현 */
    private static final List<String> NO_REST_DAY_KEYWORDS = List.of(
            "연중", "휴무없음", "휴무일없음", "없음", "상시"
    );

    /** 이 표현이 있으면 텍스트가 휴무를 설명하고 있다고 본다. */
    private static final List<String> CLOSURE_KEYWORDS = List.of(
            "휴무", "휴관", "휴일", "쉬는날", "쉬는 날", "휴점", "휴장", "closed"
    );

    /** "월,화" 처럼 요일과 구분자만으로 이루어진 텍스트 */
    private static final Pattern WEEKDAY_ONLY = Pattern.compile("^[월화수목금토일\\s,·/및]+$");

    /** 09:00~18:00 / 09:00 - 18:00 / 09:00 ~ 18:00 */
    private static final Pattern HHMM_RANGE =
            Pattern.compile("(\\d{1,2})\\s*:\\s*(\\d{2})\\s*[~\\-–—]\\s*(\\d{1,2})\\s*:\\s*(\\d{2})");

    /** 9시~18시 / 오전 9시 ~ 오후 6시 */
    private static final Pattern HOUR_RANGE =
            Pattern.compile("(오전|오후)?\\s*(\\d{1,2})\\s*시\\s*[~\\-–—]\\s*(오전|오후)?\\s*(\\d{1,2})\\s*시");

    /** "월요일", "매주 월" 처럼 요일임이 분명한 표현 */
    private static final Pattern WEEKDAY_EXPLICIT =
            Pattern.compile("([월화수목금토일])\\s*요일|매주\\s*([월화수목금토일])");

    /** 앞뒤가 다른 한글이 아닌 단독 요일 글자. "월,화 휴무"의 '화'를 잡기 위한 것 */
    private static final Pattern WEEKDAY_STANDALONE =
            Pattern.compile("(?<![가-힣])([월화수목금토일])(?![가-힣])");

    private PlaceOpenStatus() {
    }

    public static Status evaluate(String operatingHours, String restDate, LocalDateTime now) {
        if (isClosedToday(restDate, now.getDayOfWeek())) {
            return Status.CLOSED;
        }

        List<int[]> ranges = parseRanges(operatingHours);
        if (ranges.isEmpty()) {
            // 휴무일은 아니지만 운영시간을 모르는 상태. 열려 있다고 단정하지 않는다.
            return Status.UNKNOWN;
        }

        int minutesOfDay = now.getHour() * 60 + now.getMinute();
        for (int[] range : ranges) {
            if (withinRange(minutesOfDay, range[0], range[1])) {
                return Status.OPEN;
            }
        }
        return Status.CLOSED;
    }

    /**
     * 휴무일 텍스트에 오늘 요일이 들어 있는지 확인한다.
     * restDate는 TourAPI의 "쉬는날" 필드이므로 내용 자체가 휴무를 뜻한다.
     */
    static boolean isClosedToday(String restDate, DayOfWeek today) {
        if (restDate == null || restDate.isBlank()) return false;

        String text = restDate.replaceAll("<[^>]*>", " ").trim();
        if (text.isBlank()) return false;

        String normalized = text.toLowerCase(Locale.ROOT);
        for (String keyword : NO_REST_DAY_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return false;
            }
        }

        String todayLabel = weekdayLabel(today);
        if (matchesWeekday(WEEKDAY_EXPLICIT.matcher(text), todayLabel)) {
            return true;
        }

        // 단독 요일 글자는 오탐 위험이 있다("이용할 수 있습니다"의 '수").
        // 텍스트가 휴무를 설명하고 있거나, 요일과 구분자만으로 이루어진 경우에만 적용한다.
        boolean describesClosure = CLOSURE_KEYWORDS.stream().anyMatch(normalized::contains)
                || WEEKDAY_ONLY.matcher(text.trim()).matches();
        return describesClosure && matchesWeekday(WEEKDAY_STANDALONE.matcher(text), todayLabel);
    }

    private static boolean matchesWeekday(Matcher matcher, String todayLabel) {
        while (matcher.find()) {
            for (int group = 1; group <= matcher.groupCount(); group++) {
                if (todayLabel.equals(matcher.group(group))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 운영시간 텍스트에서 (시작분, 종료분) 구간들을 뽑아낸다. */
    static List<int[]> parseRanges(String operatingHours) {
        List<int[]> ranges = new ArrayList<>();
        if (operatingHours == null || operatingHours.isBlank()) return ranges;

        String text = operatingHours.replaceAll("<[^>]*>", " ");

        Matcher hhmm = HHMM_RANGE.matcher(text);
        while (hhmm.find()) {
            int start = toMinutes(Integer.parseInt(hhmm.group(1)), Integer.parseInt(hhmm.group(2)));
            int end = toMinutes(Integer.parseInt(hhmm.group(3)), Integer.parseInt(hhmm.group(4)));
            if (start >= 0 && end >= 0) ranges.add(new int[]{start, end});
        }
        if (!ranges.isEmpty()) return ranges;

        Matcher hour = HOUR_RANGE.matcher(text);
        while (hour.find()) {
            int start = toMinutes(applyMeridiem(Integer.parseInt(hour.group(2)), hour.group(1)), 0);
            int end = toMinutes(applyMeridiem(Integer.parseInt(hour.group(4)), hour.group(3)), 0);
            if (start >= 0 && end >= 0) ranges.add(new int[]{start, end});
        }
        return ranges;
    }

    /** 자정을 넘기는 구간(예: 18:00~02:00)도 처리한다. */
    private static boolean withinRange(int minutesOfDay, int start, int end) {
        if (start == end) return true;              // 24시간 운영으로 간주
        if (start < end) return minutesOfDay >= start && minutesOfDay < end;
        return minutesOfDay >= start || minutesOfDay < end;
    }

    private static int applyMeridiem(int hour, String meridiem) {
        if (meridiem == null) return hour;
        if ("오후".equals(meridiem) && hour < 12) return hour + 12;
        if ("오전".equals(meridiem) && hour == 12) return 0;
        return hour;
    }

    private static int toMinutes(int hour, int minute) {
        if (hour == 24 && minute == 0) return 24 * 60;
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return -1;
        return LocalTime.of(hour, minute).toSecondOfDay() / 60;
    }

    private static String weekdayLabel(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }
}
