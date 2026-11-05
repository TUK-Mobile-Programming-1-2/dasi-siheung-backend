package com.dasisiheung.dto.report;

import com.dasisiheung.entity.member.MemberStatus;
import com.dasisiheung.entity.report.Report;
import com.dasisiheung.entity.report.ReportAction;
import com.dasisiheung.entity.report.ReportReason;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.entity.report.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자가 보는 신고 (대상 요약 · 처리 결과 포함).
 * 사진 임시 주소는 발급 비용이 있어 상세 · 처리 응답에만 담고, 목록에는 장수만 담는다.
 */
public record AdminReportResponse(
        @Schema(description = "신고 ID", example = "1") Long id,
        @Schema(description = "대상 종류", example = "SPACE") ReportTargetType targetType,
        @Schema(description = "대상 ID", example = "10") Long targetId,
        @Schema(description = "대상 요약 (공간 이름 · 글 제목 · 댓글 앞부분 · 회원 이름, 지워졌으면 null)") String targetSummary,
        @Schema(description = "대상이 아직 보이는지 (지워졌으면 false)", example = "true") boolean targetVisible,
        @Schema(description = "대상 작성자 · 제공자 ID (지워졌으면 null)", example = "2") Long ownerId,
        @Schema(description = "대상 작성자 · 제공자 이름") String ownerName,
        @Schema(description = "대상 작성자 · 제공자 상태", example = "ACTIVE") MemberStatus ownerStatus,
        @Schema(description = "신고한 회원 이름") String reporterName,
        @Schema(description = "사유", example = "FALSE_INFORMATION") ReportReason reason,
        @Schema(description = "자세한 내용") String detail,
        @Schema(description = "증거 사진 장수", example = "2") int photoCount,
        @Schema(description = "증거 사진 (상세 · 처리 응답에만, 목록에서는 null)") List<ReportPhotoResponse> photos,
        @Schema(description = "처리 상태", example = "PENDING") ReportStatus status,
        @Schema(description = "처리 내용 (처리 전 null)") ReportAction action,
        @Schema(description = "관리자 메모") String adminNote,
        @Schema(description = "접수 시각") LocalDateTime createdAt,
        @Schema(description = "처리 시각") LocalDateTime resolvedAt
) {

    /** 목록용: 사진은 장수만. */
    public static AdminReportResponse summary(Report report, TargetView target, int photoCount) {
        return of(report, target, photoCount, null);
    }

    /** 상세용: 사진 임시 주소까지. */
    public static AdminReportResponse detail(Report report, TargetView target, List<ReportPhotoResponse> photos) {
        return of(report, target, photos.size(), List.copyOf(photos));
    }

    /** @param target 대상 (지워진 대상이면 null) */
    private static AdminReportResponse of(Report report, TargetView target, int photoCount,
                                          List<ReportPhotoResponse> photos) {
        return new AdminReportResponse(report.getId(), report.getTargetType(), report.getTargetId(),
                target == null ? null : target.summary(), target != null && target.visible(),
                target == null ? null : target.ownerId(), target == null ? null : target.ownerName(),
                target == null ? null : target.ownerStatus(), report.getReporter().getName(), report.getReason(),
                report.getDetail(), photoCount, photos, report.getStatus(), report.getAction(), report.getAdminNote(),
                report.getCreatedAt(), report.getResolvedAt());
    }

    /** 대상 요약 (DTO 가 서비스 타입에 기대지 않도록 값만 받는다). */
    public record TargetView(String summary, boolean visible, Long ownerId, String ownerName, MemberStatus ownerStatus) {
    }
}
