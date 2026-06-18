package com.ssafy.layover.place;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/map-search")
    public ResponseEntity<List<DiPlaceResponse>> getMapPlaces() {
        return ResponseEntity.ok(placeService.getMapPlaces());
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlaceSearchResponse>> searchPlaces(
            @RequestParam(defaultValue = "") String keyword) {
        return ResponseEntity.ok(placeService.searchPlaces(keyword));
    }
}
