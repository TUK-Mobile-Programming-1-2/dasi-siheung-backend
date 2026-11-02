package com.dasisiheung.dto.report;

import com.dasisiheung.entity.report.Report;
import com.dasisiheung.entity.report.ReportReason;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.entity.report.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 신고 접수 결과 (신고한 사람에게). */
public record ReportResponse(
        @Schema(description = "신고 ID", example = "1") Long id,
        @Schema(description = "대상 종류", example = "SPACE") ReportTargetType targetType,
        @Schema(description = "대상 ID", example = "10") Long targetId,
        @Schema(description = "사유", example = "FALSE_INFORMATION") ReportReason reason,
        @Schema(description = "자세한 내용") String detail,
        @Schema(description = "처리 상태", example = "PENDING") ReportStatus status,
        @Schema(description = "접수 시각") LocalDateTime createdAt
) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(report.getId(), report.getTargetType(), report.getTargetId(), report.getReason(),
                report.getDetail(), report.getStatus(), report.getCreatedAt());
    }
}
