package com.ssafy.layover.place.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PlaceSyncResult {

    private int savedCount;
    private int errorCount;
    private LocalDateTime syncedAt;

    public static PlaceSyncResult of(int savedCount, int errorCount) {
        return new PlaceSyncResult(savedCount, errorCount, LocalDateTime.now());
    }
}
