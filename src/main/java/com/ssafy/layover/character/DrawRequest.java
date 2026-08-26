package com.ssafy.layover.character;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DrawRequest {

    /** 사진을 찍는 장소. 엑스포 테마 판정에 쓴다. */
    private String placeId;
}
