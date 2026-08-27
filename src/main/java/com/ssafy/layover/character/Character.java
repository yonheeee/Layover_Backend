package com.ssafy.layover.character;

import lombok.*;

/**
 * 도감 캐릭터 마스터.
 *
 * <p>이미지 경로는 담지 않는다. 147장은 프론트엔드 번들에 있고,
 * 프론트가 {@code code}(파일명 stem)로 이미지를 찾는다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Character {

    private String id;

    /** 이미지 파일명(확장자 제외). 예: solo_char01_001 */
    private String code;

    private String name;

    /** SOLO / DUO / THEME */
    private String kind;

    /** null / BIRTHDAY / EXPO. null 인 144종만 평상시 뽑기 풀에 들어간다. */
    private String theme;

    /** 도감 그룹 키. char01, duo 는 char01+char02 */
    private String baseChar;

    private String description;

    /** @deprecated 구 해금 조건. 랜덤 뽑기로 전환하면서 사용하지 않는다. */
    @Deprecated
    private Integer requiredStamps;

    /** @deprecated 프론트가 code 로 이미지를 해석한다. */
    @Deprecated
    private String imageUrl;

    public boolean isThemed() {
        return theme != null && !theme.isBlank();
    }
}
