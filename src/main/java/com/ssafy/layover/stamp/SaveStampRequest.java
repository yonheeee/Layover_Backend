package com.ssafy.layover.stamp;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SaveStampRequest {
    private String placeId;
    private String photoUrl;

    /**
     * 스탬프를 찍는 시점의 사용자 위치.
     *
     * <p>기존에는 프론트엔드(StampTourView)에서만 100m 반경을 확인했다. 화면을 거치지 않고
     * API를 직접 호출하면 어디서든 스탬프를 받을 수 있었으므로 서버에서도 검증한다.
     */
    private Double latitude;
    private Double longitude;
}
