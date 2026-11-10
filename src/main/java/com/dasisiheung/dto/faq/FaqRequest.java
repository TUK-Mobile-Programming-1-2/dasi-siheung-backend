package com.dasisiheung.dto.faq;

import com.dasisiheung.entity.faq.Faq;
import com.dasisiheung.entity.faq.FaqCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 자주 묻는 질문 추가 · 수정 (수정은 모든 항목을 보낸다). */
public record FaqRequest(
        @Schema(description = "분류 (선택지 faqCategories)", example = "APPLICATION")
        @NotNull FaqCategory category,

        @Schema(description = "질문 (최대 200자)", example = "결제는 어떻게 하나요?")
        @NotBlank @Size(max = Faq.MAX_QUESTION_LENGTH) String question,

        @Schema(description = "답변 (최대 2000자, 줄바꿈 가능)", example = "앱에서는 결제하지 않습니다. 예약이 확정되면 공간 제공자와 1:1 대화로 결제 방법을 정해 주세요.")
        @NotBlank @Size(max = Faq.MAX_ANSWER_LENGTH) String answer
) {
}
