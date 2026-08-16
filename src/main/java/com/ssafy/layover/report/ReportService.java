package com.ssafy.layover.report;

import com.ssafy.layover.common.crypto.CryptoService;
import com.ssafy.layover.common.crypto.EncryptedValue;
import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.report.dto.ReportCreateRequest;
import com.ssafy.layover.report.dto.ReportRaw;
import com.ssafy.layover.report.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    public ApiResponse<Void> createReport(String reporterId, ReportCreateRequest request) {
        if (request.getReportedUserId() == null || request.getReportedUserId().isBlank()) {
            return ApiResponse.fail("신고할 사용자를 선택해주세요.");
        }
        if (reporterId.equals(request.getReportedUserId())) {
            return ApiResponse.fail("본인은 신고할 수 없습니다.");
        }
        if (!userRepository.existsById(request.getReportedUserId())) {
            return ApiResponse.fail("존재하지 않는 사용자입니다.");
        }
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isBlank()) {
            return ApiResponse.fail("신고 내용을 입력해주세요.");
        }

        EncryptedValue encrypted = cryptoService.encrypt(content);
        reportMapper.insert(
                UUID.randomUUID().toString(),
                reporterId,
                request.getReportedUserId(),
                encrypted.getCipherText(),
                encrypted.getIv()
        );
        return ApiResponse.success("신고가 접수되었습니다.", null);
    }

    public List<ReportResponse> getMyReports(String reporterId) {
        return reportMapper.findByReporterId(reporterId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ReportResponse toResponse(ReportRaw raw) {
        return new ReportResponse(
                raw.getId(),
                raw.getReportedUserId(),
                raw.getReportedUsername(),
                cryptoService.decrypt(raw.getEncryptedContent(), raw.getIv()),
                raw.getStatus(),
                raw.getCreatedAt()
        );
    }
}
