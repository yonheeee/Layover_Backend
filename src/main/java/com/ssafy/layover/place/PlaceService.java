package com.ssafy.layover.place;

import com.ssafy.layover.place.dto.PlaceDetailResponse;
import com.ssafy.layover.place.dto.PlaceListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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

    public Map<String, Object> getPlaces(String category, String keyword, int page, int size) {
        int offset = page * size;
        List<PlaceListResponse> content = placeMapper.findAll(category, keyword, offset, size)
                .stream()
                .map(PlaceListResponse::from)
                .toList();
        int totalCount = placeMapper.countAll(category, keyword);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalCount", totalCount);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public PlaceDetailResponse getPlaceById(String id) {
        Place place = placeMapper.findById(id);
        if (place == null) throw new NoSuchElementException("장소를 찾을 수 없습니다: " + id);
        return PlaceDetailResponse.from(place);
    }
}
