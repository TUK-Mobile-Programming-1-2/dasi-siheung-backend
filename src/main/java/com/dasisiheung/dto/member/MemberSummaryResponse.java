package com.dasisiheung.dto.member;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.Role;
import io.swagger.v3.oas.annotations.media.Schema;

public record MemberSummaryResponse(
        @Schema(description = "회원 ID", example = "1") Long id,
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "이름", example = "김시흥") String name,
        @Schema(description = "역할", example = "USER") Role role
) {

    public static MemberSummaryResponse from(Member member) {
        return new MemberSummaryResponse(member.getId(), member.getEmail(), member.getName(), member.getRole());
    }
}
