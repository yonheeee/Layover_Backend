package com.ssafy.layover.stamp;

import com.ssafy.layover.character.CharacterResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class StampResponse {

    private String id;
    private String placeId;
    private String placeName;
    private String photoUrl;
    private LocalDateTime visitedAt;
    private int stampCount;
    private CharacterResponse newCharacter;

    public static StampResponse of(Stamp stamp, int stampCount, CharacterResponse newCharacter) {
        return new StampResponse(
                stamp.getId(), stamp.getPlaceId(), stamp.getPlaceName(),
                stamp.getPhotoUrl(), stamp.getVisitedAt(), stampCount, newCharacter);
    }
}
