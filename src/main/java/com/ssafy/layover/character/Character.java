package com.ssafy.layover.character;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Character {

    private String id;
    private String name;
    private String imageUrl;
    private int requiredStamps;
    private String description;
}
