package com.ssafy.layover.character;

import com.ssafy.layover.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterMapper characterMapper;
    private final UserRepository userRepository;

    public List<CharacterResponse> getAllCharacters(String userId) {
        List<Character> all = characterMapper.findAll();
        Set<String> obtained = characterMapper.findByUserId(userId)
                .stream().map(Character::getId).collect(Collectors.toSet());
        return all.stream()
                .map(c -> CharacterResponse.of(c, obtained.contains(c.getId())))
                .collect(Collectors.toList());
    }

    public List<CharacterResponse> getMyCharacters(String userId) {
        return characterMapper.findByUserId(userId).stream()
                .map(c -> CharacterResponse.of(c, true))
                .collect(Collectors.toList());
    }
}
