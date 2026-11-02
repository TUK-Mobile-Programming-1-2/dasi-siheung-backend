package com.dasisiheung.dto.admin;

import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 관리자 대시보드 수치. 상태·지역·역할별 개수는 0 인 항목도 모두 내려준다. */
public record DashboardResponse(
        @Schema(description = "검수 대기 공간 수", example = "3") long pendingReviewCount,
        @Schema(description = "상태별 공간 수") Map<SpaceStatus, Long> spacesByStatus,
        @Schema(description = "지역별 공개 공간 수") Map<Region, Long> publishedSpacesByRegion,
        @Schema(description = "역할별 회원 수 (탈퇴 제외)") Map<Role, Long> membersByRole,
        @Schema(description = "제공자 답변을 기다리는(검토 중) 신청 수", example = "5") long pendingApplicationCount,
        @Schema(description = "상태별 신청 수") Map<ApplicationStatus, Long> applicationsByStatus,
        @Schema(description = "처리 대기 신고 수", example = "1") long pendingReportCount,
        @Schema(description = "최근 등록된 공간 5개") List<RecentSpace> recentSpaces,
        @Schema(description = "집계 시각") LocalDateTime generatedAt
) {

    public record RecentSpace(
            @Schema(description = "공간 ID") Long id,
            @Schema(description = "공간 이름") String title,
            @Schema(description = "지역") Region region,
            @Schema(description = "상태") SpaceStatus status,
            @Schema(description = "등록 시각") LocalDateTime createdAt
    ) {

        public static RecentSpace from(Space space) {
            return new RecentSpace(space.getId(), space.getTitle(), space.getRegion(), space.getStatus(),
                    space.getCreatedAt());
        }
    }
}
