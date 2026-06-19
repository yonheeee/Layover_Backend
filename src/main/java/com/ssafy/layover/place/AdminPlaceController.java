package com.ssafy.layover.place;

import com.ssafy.layover.place.dto.PlaceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/places")
@RequiredArgsConstructor
public class AdminPlaceController {

    private final TourApiService tourApiService;

    @PostMapping("/sync")
    public ResponseEntity<PlaceSyncResult> sync() {
        return ResponseEntity.ok(tourApiService.syncPlaces());
    }
}
