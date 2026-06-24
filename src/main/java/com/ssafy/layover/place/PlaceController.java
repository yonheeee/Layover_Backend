package com.ssafy.layover.place;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.place.dto.PlaceDetailResponse;
import com.ssafy.layover.place.dto.PlaceListResponse;
import com.ssafy.layover.place.dto.PlaceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final TourApiService tourApiService;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<PlaceSyncResult>> syncPlaces() {
        PlaceSyncResult result = tourApiService.syncPlaces();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/map-search")
    public ResponseEntity<List<DiPlaceResponse>> getMapPlaces() {
        return ResponseEntity.ok(placeService.getMapPlaces());
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlaceSearchResponse>> searchPlaces(
            @RequestParam(defaultValue = "") String keyword) {
        return ResponseEntity.ok(placeService.searchPlaces(keyword));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlaces(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
    	return ResponseEntity.ok(ApiResponse.success(placeService.getPlaces(category, keyword, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaceDetailResponse>> getPlaceById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(placeService.getPlaceById(id)));
    }
}
