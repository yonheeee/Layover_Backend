package com.ssafy.layover.bookmark;

import com.ssafy.layover.bookmark.dto.BookmarkResponse;
import com.ssafy.layover.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/{placeId}")
    public ResponseEntity<ApiResponse<Void>> addBookmark(
            @AuthenticationPrincipal String userId,
            @PathVariable String placeId) {
        return ResponseEntity.ok(bookmarkService.addBookmark(userId, placeId));
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<ApiResponse<Void>> removeBookmark(
            @AuthenticationPrincipal String userId,
            @PathVariable String placeId) {
        return ResponseEntity.ok(bookmarkService.removeBookmark(userId, placeId));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> getBookmarks(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.getBookmarks(userId)));
    }

    @GetMapping("/{placeId}/status")
    public ResponseEntity<ApiResponse<Boolean>> isBookmarked(
            @AuthenticationPrincipal String userId,
            @PathVariable String placeId) {
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.isBookmarked(userId, placeId)));
    }
}
