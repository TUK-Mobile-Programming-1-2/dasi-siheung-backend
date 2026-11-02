package com.dasisiheung.dto.review;

import com.dasisiheung.entity.review.UsageReview;
import com.dasisiheung.entity.space.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 이용 후기. */
public record ReviewResponse(
        @Schema(description = "후기 ID", example = "1") Long id,
        @Schema(description = "공간 ID", example = "1") Long spaceId,
        @Schema(description = "공간 이름") String spaceTitle,
        @Schema(description = "신청 ID", example = "1") Long applicationId,
        @Schema(description = "작성자 이름") String reviewerName,
        @Schema(description = "활동 종류", example = "POPUP_STORE") ActivityType activityType,
        @Schema(description = "이용 시작일") LocalDate startDate,
        @Schema(description = "이용 종료일") LocalDate endDate,
        @Schema(description = "평점 (1~5)", example = "5") int rating,
        @Schema(description = "후기") String content,
        @Schema(description = "공간 제공자 답글 (없으면 null)") String reply,
        @Schema(description = "답글 시각") LocalDateTime repliedAt,
        @Schema(description = "작성 시각") LocalDateTime createdAt,
        @Schema(description = "마지막 수정 시각") LocalDateTime updatedAt
) {

    public static ReviewResponse from(UsageReview review) {
        return new ReviewResponse(review.getId(), review.getSpace().getId(), review.getSpace().getTitle(),
                review.getApplication().getId(), review.getReviewer().getName(),
                review.getApplication().getActivityType(), review.getApplication().getStartDate(),
                review.getApplication().getEndDate(), review.getRating(), review.getContent(), review.getReply(),
                review.getRepliedAt(), review.getCreatedAt(), review.getUpdatedAt());
    }
}
