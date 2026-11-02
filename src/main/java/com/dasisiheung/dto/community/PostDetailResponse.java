package com.dasisiheung.dto.community;

import com.dasisiheung.entity.community.Post;
import com.dasisiheung.entity.community.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 게시글 상세. 댓글은 따로 페이지로 조회한다. */
public record PostDetailResponse(
        @Schema(description = "게시글 ID", example = "1") Long id,
        @Schema(description = "분류", example = "RECRUIT") PostCategory category,
        @Schema(description = "제목") String title,
        @Schema(description = "본문") String content,
        @Schema(description = "글쓴이") AuthorInfo author,
        @Schema(description = "댓글 수", example = "3") int commentCount,
        @Schema(description = "조회 수", example = "42") int viewCount,
        @Schema(description = "내가 쓴 글인지 (비로그인이면 false)", example = "false") boolean mine,
        @Schema(description = "작성 시각") LocalDateTime createdAt,
        @Schema(description = "마지막 수정 시각") LocalDateTime updatedAt
) {

    public static PostDetailResponse of(Post post, Long viewerId) {
        return new PostDetailResponse(post.getId(), post.getCategory(), post.getTitle(), post.getContent(),
                AuthorInfo.from(post.getAuthor()), post.getCommentCount(), post.getViewCount(),
                viewerId != null && post.isWrittenBy(viewerId), post.getCreatedAt(), post.getUpdatedAt());
    }
}
