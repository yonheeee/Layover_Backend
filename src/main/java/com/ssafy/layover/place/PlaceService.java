package com.ssafy.layover.place;

import com.ssafy.layover.place.dto.DiPlaceResponse;
import com.ssafy.layover.place.dto.PlaceSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;

    public List<DiPlaceResponse> getMapPlaces() {
        return placeRepository.findByIsActiveTrueAndLatitudeIsNotNullAndLongitudeIsNotNull()
                .stream()
                .map(DiPlaceResponse::new)
                .toList();
    }

    public List<PlaceSearchResponse> searchPlaces(String keyword) {
        return placeRepository.searchByKeyword(keyword)
                .stream()
                .map(PlaceSearchResponse::new)
                .toList();
    }
}
