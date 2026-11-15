package com.dasisiheung.dto.community;

import com.dasisiheung.entity.community.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CommentResponse(
        @Schema(description = "댓글 ID", example = "1") Long id,
        @Schema(description = "게시글 ID", example = "1") Long postId,
        @Schema(description = "글쓴이") AuthorInfo author,
        @Schema(description = "댓글") String content,
        @Schema(description = "작성 시각") LocalDateTime createdAt,
        @Schema(description = "작성자가 마지막으로 고친 시각 (고친 적 없으면 null). 값이 있으면 '(수정됨)' 표시")
        LocalDateTime editedAt
) {

    public static CommentResponse of(Comment comment, AuthorInfo author) {
        return new CommentResponse(comment.getId(), comment.getPost().getId(), author,
                comment.getContent(), comment.getCreatedAt(), comment.getEditedAt());
    }
}
