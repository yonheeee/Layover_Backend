package com.ssafy.layover.notice;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.notice.dto.NoticeDetailResponse;
import com.ssafy.layover.notice.dto.NoticeListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;

    public List<NoticeListResponse> getNotices() {
        return noticeMapper.findAll();
    }

    public ApiResponse<NoticeDetailResponse> getNotice(String id) {
        NoticeDetailResponse notice = noticeMapper.findById(id);
        if (notice == null) {
            return ApiResponse.fail("존재하지 않는 공지사항입니다.");
        }
        return ApiResponse.success(notice);
    }
}
