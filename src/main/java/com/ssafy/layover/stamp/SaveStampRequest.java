package com.ssafy.layover.stamp;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SaveStampRequest {

    private String placeId;

    /** 업로드가 끝난 엽서 사진 URL */
    private String photoUrl;

    /**
     * POST /api/characters/draw 로 뽑은 캐릭터 id.
     *
     * <p>사진을 다시 찍으면 클라이언트가 새로 뽑아 이 값을 갈아끼운다.
     * null 이면 서버가 저장 시점에 즉석으로 추첨한다.
     */
    private String characterId;

    /**
     * 스탬프를 찍는 시점의 사용자 위치.
     *
     * <p>화면에서만 반경을 확인하면 API를 직접 호출해 우회할 수 있으므로
     * 서버에서도 검증한다.
     */
    private Double latitude;
    private Double longitude;

    /** 측위 오차 반경(m). 촬영 직전 확인 때와 같은 값을 보낸다. */
    private Double accuracy;
}
