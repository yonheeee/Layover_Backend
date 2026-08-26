package com.ssafy.layover.character;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 캐릭터 획득 로그.
 *
 * <p>같은 캐릭터를 여러 번 뽑을 수 있으므로 (user_id, character_id) 유니크 제약이
 * 없다. 도감은 이 테이블을 character_id 로 집계해 만든다.
 *
 * <p>유니크 제약이 사라졌으므로 실수로 두 번 INSERT 해도 DB가 막아주지 않는다.
 * INSERT 는 {@code StampService.saveStamp} 트랜잭션 안에서 정확히 한 번만 해야 한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCharacter {

    private String id;
    private String userId;
    private String characterId;
    private String stampId;
    private String placeId;
    private String photoUrl;
    private LocalDateTime obtainedAt;

    public static UserCharacter of(String userId, String characterId,
                                   String stampId, String placeId, String photoUrl) {
        UserCharacter uc = new UserCharacter();
        uc.id = UUID.randomUUID().toString();
        uc.userId = userId;
        uc.characterId = characterId;
        uc.stampId = stampId;
        uc.placeId = placeId;
        uc.photoUrl = photoUrl;
        uc.obtainedAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        return uc;
    }
}
