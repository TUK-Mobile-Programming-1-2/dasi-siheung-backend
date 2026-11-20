package com.dasisiheung.dto.application;

import com.dasisiheung.entity.application.PriceOffer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** 받은 가격 제안 거절. 한마디는 선택이며 제안 이력에서 상대에게 보인다. 본문 없이 보내도 된다. */
public record RejectOfferRequest(
        @Schema(description = "거절 한마디 (선택, 최대 300자)", example = "주말이라 9만원은 어렵습니다.")
        @Size(max = PriceOffer.MAX_MESSAGE_LENGTH) String message
) {

    public static final RejectOfferRequest EMPTY = new RejectOfferRequest(null);
}
