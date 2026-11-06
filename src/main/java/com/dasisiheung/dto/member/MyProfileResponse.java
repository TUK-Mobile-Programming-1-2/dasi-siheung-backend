package com.dasisiheung.dto.member;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.MemberStatus;
import com.dasisiheung.entity.member.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MyProfileResponse(
        @Schema(description = "회원 ID", example = "1") Long id,
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "이름", example = "김시흥") String name,
        @Schema(description = "휴대폰 번호 (숫자만)", example = "01012345678") String phone,
        @Schema(description = "프로필 사진 주소 (없으면 null — 기본 이미지)") String profileImageUrl,
        @Schema(description = "역할", example = "USER") Role role,
        @Schema(description = "상태", example = "ACTIVE") MemberStatus status,
        @Schema(description = "가입 시각 (KST)", example = "2026-10-20T21:00:00") LocalDateTime createdAt
) {

    /** @param profileImageUrl 사진이 없으면 null */
    public static MyProfileResponse of(Member member, String profileImageUrl) {
        return new MyProfileResponse(member.getId(), member.getEmail(), member.getName(), member.getPhone(),
                profileImageUrl, member.getRole(), member.getStatus(), member.getCreatedAt());
    }
}
