package com.ssafy.layover.report;

import com.ssafy.layover.report.dto.ReportRaw;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    void insert(@Param("id") String id,
                @Param("reporterId") String reporterId,
                @Param("reportedUserId") String reportedUserId,
                @Param("encryptedContent") String encryptedContent,
                @Param("iv") String iv);

    List<ReportRaw> findByReporterId(@Param("reporterId") String reporterId);
}
