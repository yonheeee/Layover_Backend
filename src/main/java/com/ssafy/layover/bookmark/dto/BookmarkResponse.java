package com.ssafy.layover.bookmark.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponse {
    private String placeId;
    private String name;
    private String category;
    private String address;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
}
