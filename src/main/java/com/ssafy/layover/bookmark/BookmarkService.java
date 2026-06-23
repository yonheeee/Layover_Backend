package com.ssafy.layover.bookmark;

import com.ssafy.layover.bookmark.dto.BookmarkResponse;
import com.ssafy.layover.common.dto.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookmarkService {

    private final BookmarkMapper bookmarkMapper;

    public BookmarkService(BookmarkMapper bookmarkMapper) {
        this.bookmarkMapper = bookmarkMapper;
    }

    public ApiResponse<Void> addBookmark(String userId, String placeId) {
        if (bookmarkMapper.existsByUserIdAndPlaceId(userId, placeId)) {
            return ApiResponse.fail("이미 찜한 장소입니다.");
        }
        bookmarkMapper.insert(userId, placeId);
        return ApiResponse.success("찜 목록에 추가되었습니다.", null);
    }

    public ApiResponse<Void> removeBookmark(String userId, String placeId) {
        if (!bookmarkMapper.existsByUserIdAndPlaceId(userId, placeId)) {
            return ApiResponse.fail("찜하지 않은 장소입니다.");
        }
        bookmarkMapper.delete(userId, placeId);
        return ApiResponse.success("찜 목록에서 제거되었습니다.", null);
    }

    public List<BookmarkResponse> getBookmarks(String userId) {
        return bookmarkMapper.findByUserId(userId);
    }

    public boolean isBookmarked(String userId, String placeId) {
        return bookmarkMapper.existsByUserIdAndPlaceId(userId, placeId);
    }
}
