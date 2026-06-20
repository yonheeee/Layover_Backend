package com.ssafy.layover.inquiry;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.inquiry.dto.InquiryCreateRequest;
import com.ssafy.layover.inquiry.dto.InquiryDetailResponse;
import com.ssafy.layover.inquiry.dto.InquiryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryMapper inquiryMapper;

    public ApiResponse<Void> createInquiry(String userId, InquiryCreateRequest req) {
        inquiryMapper.insert(UUID.randomUUID().toString(), userId, req.getTitle(), req.getContent());
        return ApiResponse.success("문의가 등록되었습니다.", null);
    }

    public List<InquiryListResponse> getMyInquiries(String userId) {
        return inquiryMapper.findByUserId(userId);
    }

    public ApiResponse<InquiryDetailResponse> getInquiry(String id, String userId) {
        InquiryDetailResponse inquiry = inquiryMapper.findByIdAndUserId(id, userId);
        if (inquiry == null) {
            return ApiResponse.fail("존재하지 않거나 권한이 없습니다.");
        }
        return ApiResponse.success(inquiry);
    }
}
