package com.dasisiheung.dto.admin;

import com.dasisiheung.entity.space.ReviewChecklist;
import com.dasisiheung.entity.space.ReviewDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpaceReviewRequest(
        @Schema(description = "검수 결과: APPROVE 공개 승인 | REQUEST_REVISION 수정 요청 | HOLD 공개 보류", example = "APPROVE")
        @NotNull ReviewDecision decision,

        @Schema(description = "사유 (수정 요청·공개 보류는 필수, 제공자에게 그대로 보임)", example = "등기사항증명서와 호실이 다릅니다.")
        @Size(max = 1000) String reason,

        @Schema(description = "검수 5개 항목 확인 결과") @NotNull @Valid Checklist checklist
) {

    public ReviewChecklist toChecklist() {
        return new ReviewChecklist(checklist.authorityVerified(), checklist.addressMatched(),
                checklist.buildingUnitMatched(), checklist.areaMatched(), checklist.usageMatched());
    }

    /** 등록 정보와 제출 증빙의 일치 여부. 공개 승인은 모두 true 여야 한다. */
    public record Checklist(
            @Schema(description = "제공 권한: 공간을 제공할 권한이 증빙으로 확인됨") @NotNull Boolean authorityVerified,
            @Schema(description = "주소: 등록 주소와 증빙 일치") @NotNull Boolean addressMatched,
            @Schema(description = "건물·호실: 등록 건물·호실과 증빙 일치") @NotNull Boolean buildingUnitMatched,
            @Schema(description = "면적: 등록 면적과 증빙 일치") @NotNull Boolean areaMatched,
            @Schema(description = "용도: 등록 용도와 증빙 일치") @NotNull Boolean usageMatched
    ) {
    }
}
