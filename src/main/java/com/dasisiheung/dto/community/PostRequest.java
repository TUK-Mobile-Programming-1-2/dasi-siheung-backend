package com.dasisiheung.dto.community;

import com.dasisiheung.entity.community.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 게시글 작성 · 수정. */
public record PostRequest(
        @Schema(description = "분류 (선택지 postCategories, 공지는 관리자만)", example = "RECRUIT")
        @NotNull PostCategory category,

        @Schema(description = "제목 (최대 100자)", example = "배곧 플리마켓 같이 하실 분 구해요")
        @NotBlank @Size(max = 100) String title,

        @Schema(description = "본문 (최대 5000자)", example = "11월 둘째 주말, 핸드메이드 소품 팝업을 함께 열 팀을 찾습니다.")
        @NotBlank @Size(max = 5000) String content
) {
}
