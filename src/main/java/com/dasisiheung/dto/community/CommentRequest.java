package com.dasisiheung.dto.community;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @Schema(description = "댓글 (최대 1000자)", example = "저도 관심 있어요! 쪽지 드릴게요.")
        @NotBlank @Size(max = 1000) String content
) {
}
