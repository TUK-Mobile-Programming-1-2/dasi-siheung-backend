package com.dasisiheung.dto.report;

import com.dasisiheung.entity.report.ReportReason;
import com.dasisiheung.entity.report.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @Schema(description = "대상 종류 (선택지 reportTargetTypes)", example = "SPACE")
        @NotNull ReportTargetType targetType,

        @Schema(description = "대상 ID (공간 · 게시글 · 댓글 · 회원 ID)", example = "10")
        @NotNull Long targetId,

        @Schema(description = "사유 (선택지 reportReasons)", example = "FALSE_INFORMATION")
        @NotNull ReportReason reason,

        @Schema(description = "자세한 내용 (선택, 최대 1000자)", example = "사진과 실제 공간이 전혀 다릅니다.")
        @Size(max = 1000) String detail
) {
}
