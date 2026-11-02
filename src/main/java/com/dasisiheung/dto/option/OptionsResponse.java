package com.dasisiheung.dto.option;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 앱·관리자 웹의 선택 목록(드롭다운·칩)에 쓰는 코드와 이름. 코드는 바뀌지 않고 이름만 바뀔 수 있다. */
public record OptionsResponse(
        @Schema(description = "가입할 수 있는 역할") List<OptionItem> signupRoles,
        @Schema(description = "서비스 지역") List<OptionItem> regions,
        @Schema(description = "허용 활동 · 신청 활동 종류") List<OptionItem> activityTypes,
        @Schema(description = "보유 시설") List<OptionItem> facilities,
        @Schema(description = "건축물 용도") List<OptionItem> buildingUses,
        @Schema(description = "상권 테마") List<OptionItem> commercialThemes,
        @Schema(description = "이용 목적") List<OptionItem> usePurposes,
        @Schema(description = "권한 증빙 종류") List<OptionItem> proofTypes,
        @Schema(description = "공간 공개 상태") List<OptionItem> spaceStatuses,
        @Schema(description = "관리자 검수 결과") List<OptionItem> reviewDecisions,
        @Schema(description = "공간 검색 정렬") List<OptionItem> spaceSorts,
        @Schema(description = "신청 상태") List<OptionItem> applicationStatuses,
        @Schema(description = "가격 협의 상태") List<OptionItem> negotiationStatuses,
        @Schema(description = "가격 제안한 쪽") List<OptionItem> offerSides,
        @Schema(description = "활동 사진 시점") List<OptionItem> photoPhases,
        @Schema(description = "커뮤니티 분류") List<OptionItem> postCategories,
        @Schema(description = "신고 대상 종류") List<OptionItem> reportTargetTypes,
        @Schema(description = "신고 사유") List<OptionItem> reportReasons,
        @Schema(description = "신고 처리 상태") List<OptionItem> reportStatuses,
        @Schema(description = "신고 처리 방법") List<OptionItem> reportActions
) {
}
