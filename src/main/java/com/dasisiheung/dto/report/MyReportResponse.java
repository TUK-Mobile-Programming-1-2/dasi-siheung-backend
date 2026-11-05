package com.dasisiheung.dto.report;

import com.dasisiheung.entity.report.Report;
import com.dasisiheung.entity.report.ReportReason;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.entity.report.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 내가 한 신고 (신고한 사람에게).
 * 처리 결과는 상태만 알려주고, 관리자 메모 · 처리 내용(숨김 · 정지) · 증거 사진 주소는 주지 않는다.
 */
public record MyReportResponse(
        @Schema(description = "신고 ID", example = "1") Long id,
        @Schema(description = "대상 종류", example = "SPACE") ReportTargetType targetType,
        @Schema(description = "대상 ID", example = "10") Long targetId,
        @Schema(description = "대상 요약 (공간 이름 · 글 제목 · 댓글 앞부분 · 회원 이름, 지워졌으면 null)",
                example = "정왕역 앞 코너 상가") String targetSummary,
        @Schema(description = "사유", example = "FALSE_INFORMATION") ReportReason reason,
        @Schema(description = "자세한 내용") String detail,
        @Schema(description = "처리 상태", example = "PENDING") ReportStatus status,
        @Schema(description = "첨부한 증거 사진 장수", example = "2") int photoCount,
        @Schema(description = "접수 시각") LocalDateTime createdAt,
        @Schema(description = "처리 시각 (처리 전 null)") LocalDateTime resolvedAt
) {

    /** @param targetSummary 대상이 지워졌으면 null */
    public static MyReportResponse of(Report report, String targetSummary, int photoCount) {
        return new MyReportResponse(report.getId(), report.getTargetType(), report.getTargetId(), targetSummary,
                report.getReason(), report.getDetail(), report.getStatus(), photoCount, report.getCreatedAt(),
                report.getResolvedAt());
    }
}
