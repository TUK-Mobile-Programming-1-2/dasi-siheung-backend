package com.dasisiheung.dto.report;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MemberSuspensionResponse(
        @Schema(description = "회원 ID", example = "2") Long memberId,
        @Schema(description = "회원 이름") String name,
        @Schema(description = "상태", example = "SUSPENDED") MemberStatus status,
        @Schema(description = "정지 해제 예정 시각 (정지 중이 아니면 null)") LocalDateTime suspendedUntil
) {

    public static MemberSuspensionResponse from(Member member) {
        return new MemberSuspensionResponse(member.getId(), member.getName(), member.getStatus(),
                member.getSuspendedUntil());
    }
}
