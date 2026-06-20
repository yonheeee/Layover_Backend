package com.ssafy.layover.inquiry;

import com.ssafy.layover.inquiry.dto.InquiryDetailResponse;
import com.ssafy.layover.inquiry.dto.InquiryListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InquiryMapper {

    void insert(@Param("id") String id,
                @Param("userId") String userId,
                @Param("title") String title,
                @Param("content") String content);

    List<InquiryListResponse> findByUserId(@Param("userId") String userId);

    InquiryDetailResponse findByIdAndUserId(@Param("id") String id, @Param("userId") String userId);
}
