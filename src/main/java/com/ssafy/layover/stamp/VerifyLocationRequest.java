package com.ssafy.layover.stamp;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 촬영 직전 위치 확인 요청.
 *
 * <p>좌표는 판정에만 쓰고 어디에도 저장하지 않는다.
 */
@Getter
@NoArgsConstructor
public class VerifyLocationRequest {

    private String placeId;
    private Double latitude;
    private Double longitude;

    /**
     * 측위 오차 반경(m). {@code GeolocationCoordinates.accuracy} 값이다.
     *
     * <p>실외 GPS는 5~20m, 실내 WiFi는 20~100m, 기지국까지 떨어지면 500m를 넘는다.
     * 이 값을 보지 않으면 오차가 1km인 좌표로 100m 반경을 판정하게 된다.
     */
    private Double accuracy;
}
