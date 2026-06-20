package com.ssafy.layover.character;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCharacter {

    private String id;
    private String userId;
    private String characterId;
    private LocalDateTime obtainedAt;

    public static UserCharacter create(String userId, String characterId) {
        UserCharacter uc = new UserCharacter();
        uc.id = UUID.randomUUID().toString();
        uc.userId = userId;
        uc.characterId = characterId;
        uc.obtainedAt = LocalDateTime.now();
        return uc;
    }
}
