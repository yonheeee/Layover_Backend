package com.ssafy.layover.character;

import com.ssafy.layover.common.dto.ApiResponse;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<CharacterResponse>>> getAllCharacters(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(characterService.getAllCharacters(userId)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<CharacterResponse>>> getMyCharacters(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(characterService.getMyCharacters(userId)));
    }
}
