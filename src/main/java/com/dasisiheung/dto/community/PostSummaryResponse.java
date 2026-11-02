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
        @Schema(description = "글쓴이") AuthorInfo author,
        @Schema(description = "댓글 수", example = "3") int commentCount,
        @Schema(description = "조회 수", example = "42") int viewCount,
        @Schema(description = "작성 시각") LocalDateTime createdAt
) {

    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(post.getId(), post.getCategory(), post.getTitle(),
                AuthorInfo.from(post.getAuthor()), post.getCommentCount(), post.getViewCount(), post.getCreatedAt());
    }
}
