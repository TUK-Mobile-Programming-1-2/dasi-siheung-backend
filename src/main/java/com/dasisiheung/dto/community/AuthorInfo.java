package com.dasisiheung.dto.community;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.Role;
import io.swagger.v3.oas.annotations.media.Schema;

/** 글쓴이 공개 정보. 탈퇴한 회원은 이름이 "탈퇴한 회원"으로 보인다. */
public record AuthorInfo(
        @Schema(description = "회원 ID", example = "1") Long id,
        @Schema(description = "이름", example = "이이용") String name,
        @Schema(description = "역할 (USER · PROVIDER · ADMIN)", example = "USER") Role role
) {

    public static AuthorInfo from(Member member) {
        return new AuthorInfo(member.getId(), member.getName(), member.getRole());
    }
}
