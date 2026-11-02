package com.dasisiheung.dto.community;

import com.dasisiheung.entity.community.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CommentResponse(
        @Schema(description = "댓글 ID", example = "1") Long id,
        @Schema(description = "게시글 ID", example = "1") Long postId,
        @Schema(description = "글쓴이") AuthorInfo author,
        @Schema(description = "댓글") String content,
        @Schema(description = "작성 시각") LocalDateTime createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getPost().getId(), AuthorInfo.from(comment.getAuthor()),
                comment.getContent(), comment.getCreatedAt());
    }
}
