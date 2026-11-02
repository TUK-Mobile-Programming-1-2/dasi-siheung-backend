package com.dasisiheung.dto.report;

import com.dasisiheung.entity.report.ReportAction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 신고 처리. 이용 정지가 들어간 처리는 정지 일수가 필요하다. */
public record ResolveReportRequest(
        @Schema(description = "처리 (선택지 reportActions)", example = "HIDE_AND_SUSPEND")
        @NotNull ReportAction action,

        @Schema(description = "이용 정지 일수 (1~365, 정지가 들어간 처리만)", example = "7")
        @Min(1) @Max(365) Integer suspendDays,

        @Schema(description = "관리자 메모 (선택, 최대 500자) — 숨긴 공간의 사유로도 쓰인다", example = "허위 사진 확인, 1주 정지")
        @Size(max = 500) String note
) {

    @JsonIgnore
    @AssertTrue(message = "이용 정지가 들어간 처리는 정지 일수(1~365)를 보내야 합니다.")
    public boolean isSuspendDaysPresent() {
        return action == null || !action.isSuspendsOwner() || suspendDays != null;
    }
}
