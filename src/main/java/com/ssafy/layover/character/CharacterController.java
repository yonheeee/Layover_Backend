package com.ssafy.layover.character;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.common.exception.NotFoundException;
import com.ssafy.layover.place.Place;
import com.ssafy.layover.place.PlaceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;
    private final CharacterDrawService characterDrawService;
    private final PlaceMapper placeMapper;

    /** 전체 목록 (관리·디버그용). 도감 화면은 프론트 카탈로그를 쓴다. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CharacterResponse>>> getAllCharacters(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(characterService.getAllCharacters(userId)));
    }

    /** 내가 모은 캐릭터 (code + 중복 횟수 + 최초 획득일) */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OwnedCharacterResponse>>> getMyCharacters(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(characterService.getMyCharacters(userId)));
    }

    /**
     * 캐릭터 뽑기. 저장하지 않는다.
     *
     * <p>사진을 다시 찍을 때마다 호출해도 도감은 변하지 않는다. 실제 획득은
     * 여기서 받은 캐릭터 id 를 {@code POST /api/stamps} 에 실어 보낼 때 확정된다.
     */
    @PostMapping("/draw")
    public ResponseEntity<ApiResponse<CharacterResponse>> draw(
            @AuthenticationPrincipal String userId,
            @RequestBody DrawRequest req) {
        Place place = placeMapper.findById(req.getPlaceId());
        if (place == null) {
            throw new NotFoundException("장소를 찾을 수 없습니다.");
        }
        Character drawn = characterDrawService.draw(userId, place);
        return ResponseEntity.ok(ApiResponse.success(CharacterResponse.of(drawn, false)));
    }
}
