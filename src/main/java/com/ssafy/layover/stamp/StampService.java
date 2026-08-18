package com.ssafy.layover.stamp;

import com.ssafy.layover.character.Character;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StampService {

    private static final int EARTH_RADIUS_METERS = 6_371_000;

    private final StampMapper stampMapper;
    private final CharacterMapper characterMapper;
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

    @Transactional
    public StampResponse saveStamp(String userId, SaveStampRequest req) {
        if (stampMapper.existsByUserIdAndPlaceId(userId, req.getPlaceId())) {
            throw new DuplicateException("이미 스탬프를 획득한 장소입니다.");
        }

        verifyLocation(req);

        Stamp stamp = Stamp.create(userId, req.getPlaceId(), req.getPhotoUrl());
        stampMapper.insert(stamp);

        userRepository.incrementStampCount(userId);
        int newCount = userRepository.getStampCount(userId);

        CharacterResponse newCharacter = null;
        Character character = characterMapper.findByRequiredStamps(newCount);
        if (character != null && !characterMapper.existsUserCharacter(userId, character.getId())) {
            characterMapper.insertUserCharacter(UserCharacter.create(userId, character.getId()));
            newCharacter = CharacterResponse.of(character, true);
        }

        return StampResponse.of(stamp, newCount, newCharacter);
    }

    /**
     * 사용자가 실제로 그 장소 근처에 있는지 확인한다.
     * 장소에 좌표가 없으면 검증할 방법이 없으므로 통과시킨다.
     */
    private void verifyLocation(SaveStampRequest req) {
        if (!verificationEnabled) {
            log.warn("[Stamp] 위치 검증이 꺼져 있습니다. 배포 환경에서는 stamp.verification.enabled=true 여야 합니다.");
            return;
        }

        Place place = placeMapper.findById(req.getPlaceId());
        if (place == null) {
            throw new NotFoundException("장소를 찾을 수 없습니다.");
        }
        if (place.getLatitude() == null || place.getLongitude() == null) {
            log.info("[Stamp] 장소 {}에 좌표가 없어 위치 검증을 건너뜁니다.", place.getName());
            return;
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
