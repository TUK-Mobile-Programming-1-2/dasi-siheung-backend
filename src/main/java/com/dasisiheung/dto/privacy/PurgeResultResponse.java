package com.dasisiheung.dto.privacy;

import com.dasisiheung.entity.privacy.PrivacyPurgeLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 파기 실행 결과 · 기록. */
public record PurgeResultResponse(
        @Schema(description = "기록 ID", example = "1") Long id,
        @Schema(description = "실행한 관리자 ID (자동 실행이면 null)", example = "3") Long executedBy,
        @Schema(description = "신청서 비식별화 기준일") LocalDate applicationCutoff,
        @Schema(description = "통계에 더한 확정 예약 수", example = "5") int countedBookings,
        @Schema(description = "비식별화한 신청 수", example = "12") int anonymizedApplications,
        @Schema(description = "삭제한 대화 수", example = "3") int deletedConversations,
        @Schema(description = "삭제한 활동 사진 수", example = "20") int deletedActivityPhotos,
        @Schema(description = "실행 시각") LocalDateTime executedAt
) {

    public static PurgeResultResponse from(PrivacyPurgeLog log) {
        return new PurgeResultResponse(log.getId(), log.getExecutedBy(), log.getApplicationCutoff(),
                log.getCountedBookings(), log.getAnonymizedApplications(), log.getDeletedConversations(),
                log.getDeletedActivityPhotos(), log.getExecutedAt());
    }
}
