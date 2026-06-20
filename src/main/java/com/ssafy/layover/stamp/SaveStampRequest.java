package com.ssafy.layover.stamp;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SaveStampRequest {
    private String placeId;
    private String photoUrl;
}
