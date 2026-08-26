package com.ssafy.layover.character;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 도감에서 내가 보유한 캐릭터 한 종.
 *
 * <p>전체 147종 목록은 프론트엔드가 collection 폴더에서 유도하므로
 * 서버는 보유분과 중복 횟수만 내려준다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OwnedCharacterResponse {

    private String code;

    /** 중복 포함 획득 횟수 */
    private int count;

    private LocalDateTime firstObtainedAt;
}
