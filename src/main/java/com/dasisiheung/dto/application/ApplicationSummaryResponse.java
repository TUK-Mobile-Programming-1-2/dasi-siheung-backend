package com.dasisiheung.dto.application;

import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.application.PriceNegotiationStatus;
import com.dasisiheung.entity.space.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 신청 목록 항목 (보낸 신청 · 받은 신청 공통). */
public record ApplicationSummaryResponse(
        @Schema(description = "신청 ID", example = "1") Long id,
        @Schema(description = "신청 상태", example = "PENDING") ApplicationStatus status,
        @Schema(description = "공간 ID", example = "1") Long spaceId,
        @Schema(description = "공간 이름") String spaceTitle,
        @Schema(description = "공간 대표 사진 주소 (없으면 null)") String thumbnailUrl,
        @Schema(description = "신청자 이름") String applicantName,
        @Schema(description = "활동 종류", example = "POPUP_STORE") ActivityType activityType,
        @Schema(description = "활동 이름") String activityTitle,
        @Schema(description = "이용 시작일") LocalDate startDate,
        @Schema(description = "이용 종료일") LocalDate endDate,
        @Schema(description = "현재 합의 금액(원)", example = "100000") int totalPrice,
        @Schema(description = "가격 협의 상태", example = "WAITING_PROVIDER") PriceNegotiationStatus negotiationStatus,
        @Schema(description = "신청자 ↔ 공간 제공자 1:1 대화 ID (아직 없으면 null)", example = "3") Long conversationId,
        @Schema(description = "신청 시각") LocalDateTime createdAt
) {

    public static ApplicationSummaryResponse of(Application application, String thumbnailUrl, Long conversationId) {
        return new ApplicationSummaryResponse(application.getId(), application.getStatus(),
                application.getSpace().getId(), application.getSpace().getTitle(), thumbnailUrl,
                application.getApplicant().getName(), application.getActivityType(), application.getActivityTitle(),
                application.getStartDate(), application.getEndDate(), application.getTotalPrice(),
                application.getNegotiationStatus(), conversationId, application.getCreatedAt());
    }
}
