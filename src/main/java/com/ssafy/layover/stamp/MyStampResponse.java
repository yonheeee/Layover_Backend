package com.ssafy.layover.stamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 내가 찍은 스탬프 한 건.
 *
 * <p>엔티티({@link Stamp})를 그대로 내보내지 않는 이유가 둘이다.
 * 하나는 호출자 본인의 {@code userId} 가 응답에 딸려 나가는 것,
 * 다른 하나는 지도 핀에 필요한 좌표가 엔티티에는 없다는 것이다.
 * 좌표는 {@code places} 를 조인해 채운다.
 *
 * <p>마이페이지의 인증 사진 목록과 스탬프 지도가 이 응답만으로 그려진다.
 * 예전에는 localStorage 에만 목록이 있어서, 사진은 S3 에 멀쩡히 있는데
 * 다른 기기로 로그인하면 그리드와 지도가 텅 비었다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyStampResponse {

    private String id;
    private String placeId;
    private String placeName;

    /** 화면에서 이모지를 고르는 데 쓴다. FOOD / CAFE / NATURE / CULTURE / ... */
    private String category;

    private String photoUrl;
    private LocalDateTime visitedAt;

    /** 지도 핀 좌표. 장소에 좌표가 없으면 null */
    private Double latitude;
    private Double longitude;
}
