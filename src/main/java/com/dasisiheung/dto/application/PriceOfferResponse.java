package com.dasisiheung.dto.application;

import com.dasisiheung.entity.application.OfferSide;
import com.dasisiheung.entity.application.PriceOffer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 가격 협의 이력 한 건. */
public record PriceOfferResponse(
        @Schema(description = "제안 ID", example = "1") Long id,
        @Schema(description = "제안한 쪽", example = "APPLICANT") OfferSide proposerSide,
        @Schema(description = "제안한 회원 이름") String proposerName,
        @Schema(description = "제안 금액(원, 기간 합계)", example = "90000") int amount,
        @Schema(description = "한마디") String message,
        @Schema(description = "상대가 수락한 시각 (수락 전이면 null)") LocalDateTime acceptedAt,
        @Schema(description = "제안 시각") LocalDateTime createdAt
) {

    public static PriceOfferResponse from(PriceOffer offer) {
        return new PriceOfferResponse(offer.getId(), offer.getProposerSide(), offer.getProposer().getName(),
                offer.getAmount(), offer.getMessage(), offer.getAcceptedAt(), offer.getCreatedAt());
    }
}
