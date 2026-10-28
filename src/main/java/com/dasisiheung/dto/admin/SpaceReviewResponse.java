package com.dasisiheung.dto.admin;

import com.dasisiheung.entity.space.ReviewChecklist;
import com.dasisiheung.entity.space.ReviewDecision;
import com.dasisiheung.entity.space.SpaceReview;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 검수 이력 한 건. */
public record SpaceReviewResponse(
        @Schema(description = "검수 ID", example = "1") Long id,
        @Schema(description = "검수 결과", example = "REQUEST_REVISION") ReviewDecision decision,
        @Schema(description = "사유") String reason,
        @Schema(description = "검수 항목 결과") SpaceReviewRequest.Checklist checklist,
        @Schema(description = "검수한 관리자 이름", example = "관리자") String adminName,
        @Schema(description = "검수 시각") LocalDateTime reviewedAt
) {

    public static SpaceReviewResponse from(SpaceReview review) {
        ReviewChecklist checklist = review.getChecklist();
        return new SpaceReviewResponse(review.getId(), review.getDecision(), review.getReason(),
                new SpaceReviewRequest.Checklist(checklist.isAuthorityVerified(), checklist.isAddressMatched(),
                        checklist.isBuildingUnitMatched(), checklist.isAreaMatched(), checklist.isUsageMatched()),
                review.getAdmin().getName(), review.getCreatedAt());
    }
}
