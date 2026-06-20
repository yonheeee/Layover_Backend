package com.ssafy.layover.notice;

import com.ssafy.layover.notice.dto.NoticeDetailResponse;
import com.ssafy.layover.notice.dto.NoticeListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {

    List<NoticeListResponse> findAll();

    NoticeDetailResponse findById(@Param("id") String id);
}
