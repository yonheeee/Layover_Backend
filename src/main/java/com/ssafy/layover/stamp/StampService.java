package com.ssafy.layover.stamp;

import com.ssafy.layover.character.Character;
import com.ssafy.layover.character.CharacterMapper;
import com.ssafy.layover.character.CharacterResponse;
import com.ssafy.layover.character.UserCharacter;
import com.ssafy.layover.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StampService {

    private final StampMapper stampMapper;
    private final CharacterMapper characterMapper;
    private final UserRepository userRepository;

    @Transactional
    public StampResponse saveStamp(String userId, SaveStampRequest req) {
        if (stampMapper.existsByUserIdAndPlaceId(userId, req.getPlaceId())) {
            throw new IllegalStateException("이미 스탬프를 획득한 장소입니다.");
        }

        Stamp stamp = Stamp.create(userId, req.getPlaceId(), req.getPhotoUrl());
        stampMapper.insert(stamp);

        userRepository.incrementStampCount(userId);
        int newCount = userRepository.getStampCount(userId);

        CharacterResponse newCharacter = null;
        Character character = characterMapper.findByRequiredStamps(newCount);
        if (character != null && !characterMapper.existsUserCharacter(userId, character.getId())) {
            characterMapper.insertUserCharacter(UserCharacter.create(userId, character.getId()));
            newCharacter = CharacterResponse.of(character, true);
        }

        return StampResponse.of(stamp, newCount, newCharacter);
    }

    public List<Stamp> getMyStamps(String userId) {
        return stampMapper.findByUserId(userId);
    }
}
