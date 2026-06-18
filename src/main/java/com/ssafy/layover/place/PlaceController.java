package com.ssafy.layover.place;

import com.ssafy.layover.place.dto.DiPlaceResponse;
import com.ssafy.layover.place.dto.PlaceSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    // MapView 검색 패널용 — 지도에 띄울 전체 장소 목록
    @GetMapping("/map-search")
    public ResponseEntity<List<DiPlaceResponse>> getMapPlaces() {
        return ResponseEntity.ok(placeService.getMapPlaces());
    }

    // CourseResultView 장소 추가 모달용 — 키워드 검색
    @GetMapping("/search")
    public ResponseEntity<List<PlaceSearchResponse>> searchPlaces(
            @RequestParam(defaultValue = "") String keyword) {
        return ResponseEntity.ok(placeService.searchPlaces(keyword));
    }
}
