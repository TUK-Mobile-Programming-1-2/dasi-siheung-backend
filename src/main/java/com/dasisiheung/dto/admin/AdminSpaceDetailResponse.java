package com.dasisiheung.dto.admin;

import com.dasisiheung.dto.space.ProviderSpaceResponse;
import com.dasisiheung.entity.member.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 관리자 검수 화면 — 공간 정보 · 증빙(임시 주소) · 제공자 · 검수 이력 · 공식 정보 조회 링크. */
public record AdminSpaceDetailResponse(
        @Schema(description = "공간 정보 (사진·증빙 포함)") ProviderSpaceResponse space,
        @Schema(description = "공간 제공자") ProviderInfo provider,
        @Schema(description = "검수 이력 (최근 순)") List<SpaceReviewResponse> reviews,
        @Schema(description = "공식 정보 조회 링크 (보조 자료, 검수 기준 아님)") List<ReferenceLink> referenceLinks
) {

    public record ProviderInfo(
            @Schema(description = "회원 ID") Long id,
            @Schema(description = "이름") String name,
            @Schema(description = "이메일") String email,
            @Schema(description = "휴대폰 번호") String phone
    ) {

        public static ProviderInfo from(Member member) {
            return new ProviderInfo(member.getId(), member.getName(), member.getEmail(), member.getPhone());
        }
    }

    public record ReferenceLink(
            @Schema(description = "이름", example = "건축물대장 열람 (정부24)") String name,
            @Schema(description = "주소") String url
    ) {
    }
}
