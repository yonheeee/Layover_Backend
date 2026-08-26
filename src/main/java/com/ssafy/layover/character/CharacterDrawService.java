package com.ssafy.layover.character;

import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.place.Place;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 캐릭터 뽑기.
 *
 * <p><b>이 서비스는 아무것도 저장하지 않는다.</b> 사진을 다시 찍을 때마다
 * {@code draw()} 를 호출해도 도감은 변하지 않는다. 실제 획득은
 * {@code StampService.saveStamp} 트랜잭션 안에서 확정된다.
 *
 * <p>추첨은 2단계다.
 * <ol>
 *   <li>테마(생일/엑스포)가 활성이면 {@code themeProbability} 확률로 테마 카드</li>
 *   <li>그 외에는 테마가 없는 144종에서 균등 추첨</li>
 * </ol>
 * 테마 카드는 평상시 풀에서 아예 빠지므로 조건이 맞을 때만 나온다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterDrawService {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private static final String THEME_BIRTHDAY = "BIRTHDAY";
    private static final String THEME_EXPO = "EXPO";
    private static final int EARTH_RADIUS_METERS = 6_371_000;

    private final CharacterMapper characterMapper;
    private final UserRepository userRepository;

    /** 테마가 활성일 때 테마 카드가 나올 확률 */
    @Value("${character.theme.probability:0.7}")
    private double themeProbability;

    @Value("${character.theme.expo.latitude:36.3746}")
    private double expoLatitude;

    @Value("${character.theme.expo.longitude:127.3894}")
    private double expoLongitude;

    @Value("${character.theme.expo.radius-meters:1500}")
    private double expoRadiusMeters;

    /**
     * 캐릭터 한 장을 뽑는다. 저장하지 않는다.
     *
     * @param place 사진을 찍는 장소. null 이면 엑스포 테마는 비활성으로 본다.
     */
    public Character draw(String userId, Place place) {
        List<String> themes = activeThemes(userId, place);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        if (!themes.isEmpty() && rnd.nextDouble() < themeProbability) {
            List<Character> themed = characterMapper.findByThemes(themes);
            if (!themed.isEmpty()) {
                return themed.get(rnd.nextInt(themed.size()));
            }
            log.warn("[Draw] 테마 {} 가 활성이지만 해당 카드가 없습니다. 일반 풀로 넘어갑니다.", themes);
        }

        List<Character> pool = characterMapper.findDrawPool();
        if (pool.isEmpty()) {
            throw new IllegalStateException(
                    "뽑을 수 있는 캐릭터가 없습니다. character_seed.sql 을 적용했는지 확인하세요.");
        }
        return pool.get(rnd.nextInt(pool.size()));
    }

    /**
     * 클라이언트가 지정한 캐릭터가 지금 획득 가능한지 확인한다.
     *
     * <p>일반 144종은 확률도 가치도 모두 같아서 그중 하나를 골라도 얻는 게 없다.
     * 검증이 필요한 건 조건부로만 나오는 테마 카드뿐이다.
     */
    public void validate(String userId, Place place, Character chosen) {
        if (chosen == null) {
            throw new IllegalArgumentException("캐릭터 정보가 올바르지 않습니다.");
        }
        if (!chosen.isThemed()) {
            return;
        }
        if (!activeThemes(userId, place).contains(chosen.getTheme())) {
            throw new IllegalArgumentException("지금은 획득할 수 없는 테마 캐릭터입니다.");
        }
    }

    /** 지금 이 유저·이 장소에서 활성인 테마 목록 */
    public List<String> activeThemes(String userId, Place place) {
        List<String> themes = new ArrayList<>();
        if (isBirthday(userId)) {
            themes.add(THEME_BIRTHDAY);
        }
        if (isExpo(place)) {
            themes.add(THEME_EXPO);
        }
        return themes;
    }

    /**
     * 오늘이 생일인지. 연도는 보지 않는다.
     *
     * <p>2/29 생일은 평년에 영영 오지 않으므로 3/1 로 인정한다.
     */
    private boolean isBirthday(String userId) {
        LocalDate birth = userRepository.findBirthDate(userId);
        if (birth == null) {
            return false;
        }
        LocalDate today = LocalDate.now(SEOUL);
        if (birth.getMonthValue() == today.getMonthValue()
                && birth.getDayOfMonth() == today.getDayOfMonth()) {
            return true;
        }
        return birth.getMonthValue() == 2 && birth.getDayOfMonth() == 29
                && today.getMonthValue() == 3 && today.getDayOfMonth() == 1
                && !today.isLeapYear();
    }

    /**
     * 엑스포과학공원 일대인지.
     *
     * <p>places 에 태그 컬럼을 두는 대신 좌표 반경으로 판정한다. places 는 매일
     * 03:00 TourAPI 동기화로 덮어써지므로 컬럼을 추가하면 유실 위험이 있고,
     * 반경 방식은 한빛탑·엑스포다리·신세계 아트앤사이언스처럼 이름에 '엑스포'가
     * 없는 주변 명소까지 자연스럽게 포함한다.
     */
    private boolean isExpo(Place place) {
        if (place == null || place.getLatitude() == null || place.getLongitude() == null) {
            return false;
        }
        double distance = distanceMeters(
                place.getLatitude().doubleValue(), place.getLongitude().doubleValue(),
                expoLatitude, expoLongitude);
        return distance <= expoRadiusMeters;
    }

    /** StampService 와 같은 계산. 패키지가 달라 중복을 감수한다. */
    private static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

}
