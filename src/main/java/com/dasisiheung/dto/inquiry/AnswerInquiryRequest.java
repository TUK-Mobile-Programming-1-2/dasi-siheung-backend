package com.dasisiheung.dto.inquiry;

import com.dasisiheung.entity.inquiry.Inquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 관리자 답변 등록. 답변은 한 번만 할 수 있다. */
public record AnswerInquiryRequest(
        @Schema(description = "답변 내용 (최대 2000자)", example = "확인해 보니 이전 비밀번호로 저장된 자동 로그인 정보 때문이었어요. 앱을 다시 실행해 주세요.")
        @NotBlank @Size(max = Inquiry.MAX_ANSWER_LENGTH) String answer
) {
}
