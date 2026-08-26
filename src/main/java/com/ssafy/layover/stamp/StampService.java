package com.ssafy.layover.stamp;

import com.ssafy.layover.character.Character;
import com.ssafy.layover.character.CharacterDrawService;
import com.ssafy.layover.character.CharacterMapper;
import com.ssafy.layover.character.CharacterResponse;
import com.ssafy.layover.character.UserCharacter;
import com.ssafy.layover.common.exception.DuplicateException;
import com.ssafy.layover.common.exception.NotFoundException;
import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.place.Place;
import com.ssafy.layover.place.PlaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StampService {

    private static final int EARTH_RADIUS_METERS = 6_371_000;

    private final StampMapper stampMapper;
    private final CharacterMapper characterMapper;
    private final CharacterDrawService characterDrawService;
    private final UserRepository userRepository;
    private final PlaceMapper placeMapper;

    /**
     * 위치 검증 사용 여부. 로컬 개발이나 시연 환경에서만 끈다.
     * 배포 환경에서는 반드시 true를 유지해야 한다.
     */
    @Value("${stamp.verification.enabled:true}")
    private boolean verificationEnabled;

    /** 스탬프를 인정하는 반경(m). 프론트엔드와 같은 값을 쓴다. */
    @Value("${stamp.verification.radius-meters:100}")
    private double verificationRadiusMeters;

    /**
     * 스탬프를 저장하고 캐릭터를 확정한다.
     *
     * <p>도감 해금이 일어나는 유일한 지점이다. 사진을 다시 찍는 동안에는
     * {@code /api/characters/draw} 가 아무것도 저장하지 않다가, 사용자가
     * 저장을 누른 이 시점에 한 번만 {@code user_characters} 에 기록된다.
     *
     * <p>같은 장소는 <b>하루에 한 번</b> 찍을 수 있다. 예전에는 날짜 조건이
     * 없어서 한 번 다녀온 장소는 영영 다시 찍을 수 없었다.
     */
    @Transactional
    public StampResponse saveStamp(String userId, SaveStampRequest req) {
        LocalDate today = LocalDate.now(CharacterDrawService.SEOUL);
        if (stampMapper.existsByUserIdAndPlaceIdBetween(
                userId, req.getPlaceId(),
                today.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
            throw new DuplicateException("오늘 이미 방문한 장소입니다.");
        }

        Place place = verifyLocation(req);

        Character chosen = resolveCharacter(userId, place, req.getCharacterId());

        Stamp stamp = Stamp.create(userId, req.getPlaceId(), req.getPhotoUrl());
        try {
            stampMapper.insert(stamp);
        } catch (DuplicateKeyException e) {
            // 애플리케이션 검사만으로는 동시 요청을 막을 수 없다.
            // uq_stamps_user_place_day 유니크 제약이 실제 방어선이다.
            throw new DuplicateException("오늘 이미 방문한 장소입니다.");
        }

        userRepository.incrementStampCount(userId);
        characterMapper.insertUserCharacter(UserCharacter.of(
                userId, chosen.getId(), stamp.getId(), place.getId(), req.getPhotoUrl()));

        return StampResponse.of(stamp, userRepository.getStampCount(userId),
                CharacterResponse.of(chosen, true));
    }

    /**
     * 저장할 캐릭터를 정한다.
     *
     * <p>클라이언트가 뽑아 온 캐릭터를 그대로 쓰되, 조건부로만 나오는 테마
     * 카드인지 검증한다. 일반 144종은 확률도 가치도 모두 같아서 그중 무엇을
     * 지정하든 얻는 게 없으므로 따로 막지 않는다.
     */
    private Character resolveCharacter(String userId, Place place, String characterId) {
        Character chosen = (characterId != null && !characterId.isBlank())
                ? characterMapper.findById(characterId)
                : null;

        if (chosen == null) {
            // draw 를 거치지 않고 바로 저장한 경우 (구버전 클라이언트 등)
            chosen = characterDrawService.draw(userId, place);
        }
        characterDrawService.validate(userId, place, chosen);
        return chosen;
    }

    /**
     * 사용자가 실제로 그 장소 근처에 있는지 확인한다.
     * 장소에 좌표가 없으면 검증할 방법이 없으므로 통과시킨다.
     *
     * @return 조회한 장소. 호출부에서 재조회하지 않도록 그대로 돌려준다.
     */
    private Place verifyLocation(SaveStampRequest req) {
        Place place = placeMapper.findById(req.getPlaceId());
        if (place == null) {
            throw new NotFoundException("장소를 찾을 수 없습니다.");
        }

        if (!verificationEnabled) {
            log.warn("[Stamp] 위치 검증이 꺼져 있습니다. 배포 환경에서는 stamp.verification.enabled=true 여야 합니다.");
            return place;
        }

        if (place.getLatitude() == null || place.getLongitude() == null) {
            log.info("[Stamp] 장소 {}에 좌표가 없어 위치 검증을 건너뜁니다.", place.getName());
            return place;
        }

        if (req.getLatitude() == null || req.getLongitude() == null) {
            throw new IllegalArgumentException("위치 정보가 필요합니다. 위치 권한을 허용해주세요.");
        }

        double distance = distanceMeters(
                req.getLatitude(), req.getLongitude(),
                place.getLatitude().doubleValue(), place.getLongitude().doubleValue());

        if (distance > verificationRadiusMeters) {
            throw new IllegalArgumentException(
                    String.format("장소에서 너무 멀리 있습니다. (약 %.0fm 떨어져 있어요)", distance));
        }
        return place;
    }

    static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public List<Stamp> getMyStamps(String userId) {
        return stampMapper.findByUserId(userId);
    }
}
