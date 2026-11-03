package com.dasisiheung.dto.privacy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 지금 파기를 실행하면 처리될 양 (실행 전 확인용). */
public record PurgePreviewResponse(
        @Schema(description = "이 날짜 전에 이용이 끝난 신청서를 비식별화", example = "2025-10-20") LocalDate applicationCutoff,
        @Schema(description = "비식별화할 신청 수", example = "12") long applicationsToAnonymize,
        @Schema(description = "이 시각 전부터 대화가 없던 1:1 대화를 삭제") LocalDateTime conversationCutoff,
        @Schema(description = "삭제할 대화 수", example = "3") long conversationsToDelete,
        @Schema(description = "이 날짜 전에 이용이 끝난 예약의 활동 사진을 삭제", example = "2026-07-22") LocalDate activityPhotoCutoff,
        @Schema(description = "삭제할 활동 사진 수", example = "20") long activityPhotosToDelete
) {
}
