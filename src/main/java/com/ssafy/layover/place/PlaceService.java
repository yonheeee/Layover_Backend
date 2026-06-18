package com.ssafy.layover.place;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceMapper placeMapper;

    public List<DiPlaceResponse> getMapPlaces() {
        return placeMapper.findAllWithLocation()
                .stream()
                .map(DiPlaceResponse::new)
                .toList();
    }

    public List<PlaceSearchResponse> searchPlaces(String keyword) {
        return placeMapper.searchByKeyword(keyword)
                .stream()
                .map(PlaceSearchResponse::new)
                .toList();
    }
}
