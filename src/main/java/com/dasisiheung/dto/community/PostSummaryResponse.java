package com.dasisiheung.dto.community;

import com.dasisiheung.entity.community.Post;
import com.dasisiheung.entity.community.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 게시글 목록 항목. */
public record PostSummaryResponse(
        @Schema(description = "게시글 ID", example = "1") Long id,
        @Schema(description = "분류", example = "RECRUIT") PostCategory category,
        @Schema(description = "제목") String title,
        @Schema(description = "본문 미리보기 — 줄바꿈 · 연속 공백을 공백 하나로 줄인 앞 100자",
                example = "11월 둘째 주말, 핸드메이드 소품 팝업을 함께 열 팀을 찾습니다.") String contentPreview,
        @Schema(description = "썸네일 — 처음 올린 사진 주소 (사진이 없으면 null)") String thumbnailUrl,
        @Schema(description = "글쓴이") AuthorInfo author,
        @Schema(description = "댓글 수", example = "3") int commentCount,
        @Schema(description = "조회 수", example = "42") int viewCount,
        @Schema(description = "작성 시각") LocalDateTime createdAt
) {

    /** @param thumbnailUrl 사진이 없으면 null */
    public static PostSummaryResponse of(Post post, AuthorInfo author, String thumbnailUrl) {
        return new PostSummaryResponse(post.getId(), post.getCategory(), post.getTitle(), post.contentPreview(),
                thumbnailUrl, author, post.getCommentCount(), post.getViewCount(), post.getCreatedAt());
    }
}
