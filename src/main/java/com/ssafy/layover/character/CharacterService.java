package com.ssafy.layover.character;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterMapper characterMapper;

    /**
     * 전체 캐릭터 목록.
     *
     * <p>도감 화면은 이 API를 쓰지 않는다. 프론트가 collection 폴더에서 147종을
     * 직접 유도하기 때문이다. 관리·디버그용으로 남겨둔다.
     */
    public List<CharacterResponse> getAllCharacters(String userId) {
        Set<String> ownedCodes = characterMapper.findOwnedByUserId(userId)
                .stream().map(OwnedCharacterResponse::getCode).collect(Collectors.toSet());
        return characterMapper.findAll().stream()
                .map(c -> CharacterResponse.of(c, ownedCodes.contains(c.getCode())))
                .collect(Collectors.toList());
    }

    /** 내가 모은 캐릭터. code 단위로 집계되며 중복 횟수를 포함한다. */
    public List<OwnedCharacterResponse> getMyCharacters(String userId) {
        return characterMapper.findOwnedByUserId(userId);
    }
}
