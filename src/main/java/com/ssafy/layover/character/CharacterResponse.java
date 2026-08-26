package com.ssafy.layover.character;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 캐릭터 한 종의 정보.
 *
 * <p>{@code imageUrl} 대신 {@code code} 를 내려준다. 프론트엔드가
 * {@code data/characterImages.ts} 에서 code 로 번들 이미지를 찾는다.
 */
@Getter
@AllArgsConstructor
public class CharacterResponse {

    private String id;
    private String code;
    private String name;
    private String kind;
    private String theme;
    private String baseChar;
    private String description;
    private boolean obtained;

    public static CharacterResponse of(Character c, boolean obtained) {
        return new CharacterResponse(
                c.getId(), c.getCode(), c.getName(), c.getKind(),
                c.getTheme(), c.getBaseChar(), c.getDescription(), obtained);
    }
}
