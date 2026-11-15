package com.dasisiheung.dto.member;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.MemberBlock;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 내가 차단한 회원. 탈퇴한 회원은 이름이 "탈퇴한 회원"으로 보인다. */
public record BlockedMemberResponse(
        @Schema(description = "차단한 회원 ID (대화의 counterpart.id, 글 · 댓글의 author.id 와 같은 값)", example = "7")
        Long id,
        @Schema(description = "이름", example = "시흥러버") String name,
        @Schema(description = "프로필 사진 주소 (없거나 탈퇴했으면 null)") String profileImageUrl,
        @Schema(description = "차단한 시각") LocalDateTime blockedAt
) {

    /** @param profileImageUrl 사진이 없으면 null */
    public static BlockedMemberResponse of(MemberBlock block, String profileImageUrl) {
        Member blocked = block.getBlocked();
        return new BlockedMemberResponse(blocked.getId(), blocked.getName(), profileImageUrl, block.getCreatedAt());
    }
}
