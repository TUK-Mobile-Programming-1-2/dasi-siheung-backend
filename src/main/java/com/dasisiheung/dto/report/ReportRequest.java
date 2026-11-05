package com.dasisiheung.dto.report;

import com.dasisiheung.entity.report.ReportReason;
import com.dasisiheung.entity.report.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 신고 내용. multipart/form-data 의 파트로 받는다.
 * 사진까지 한 객체로 받아야 Swagger 명세에 텍스트 파트와 파일 파트가 함께 나온다.
 */
public record ReportRequest(
        @Schema(description = "대상 종류 (선택지 reportTargetTypes)", example = "SPACE")
        @NotNull ReportTargetType targetType,

        @Schema(description = "대상 ID (공간 · 게시글 · 댓글 · 회원 ID)", example = "10")
        @NotNull Long targetId,

        @Schema(description = "사유 (선택지 reportReasons)", example = "FALSE_INFORMATION")
        @NotNull ReportReason reason,

        @Schema(description = "자세한 내용 (선택, 최대 1000자)", example = "사진과 실제 공간이 전혀 다릅니다.")
        @Size(max = 1000) String detail,

        @Schema(description = "증거 사진 (선택, 같은 이름으로 여러 개, 최대 5장, JPG·PNG·WEBP 10MB). 관리자만 볼 수 있습니다.")
        List<MultipartFile> photos
) {

    /** 앱 · Swagger 가 파일 없이 보내는 빈 파트는 사진이 아니므로 뺀다. */
    public List<MultipartFile> attachedPhotos() {
        return photos == null ? List.of() : photos.stream().filter(file -> file != null && !file.isEmpty()).toList();
    }
}
