package com.dasisiheung.dto.faq;

import com.dasisiheung.entity.faq.Faq;
import com.dasisiheung.entity.faq.FaqCategory;
import io.swagger.v3.oas.annotations.media.Schema;

public record FaqResponse(
        @Schema(description = "자주 묻는 질문 ID", example = "1") Long id,
        @Schema(description = "분류 (화면 이름은 선택지 faqCategories)", example = "APPLICATION") FaqCategory category,
        @Schema(description = "질문", example = "결제는 어떻게 하나요?") String question,
        @Schema(description = "답변 (줄바꿈 포함 가능)") String answer
) {

    public static FaqResponse from(Faq faq) {
        return new FaqResponse(faq.getId(), faq.getCategory(), faq.getQuestion(), faq.getAnswer());
    }
}
