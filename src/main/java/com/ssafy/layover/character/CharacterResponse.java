package com.ssafy.layover.character;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CharacterResponse {

    private String id;
    private String name;
    private String imageUrl;
    private int requiredStamps;
    private String description;
    private boolean obtained;

    public static CharacterResponse of(Character c, boolean obtained) {
        return new CharacterResponse(
                c.getId(), c.getName(), c.getImageUrl(),
                c.getRequiredStamps(), c.getDescription(), obtained);
    }
}
