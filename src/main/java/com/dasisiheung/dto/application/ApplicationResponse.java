package com.dasisiheung.dto.application;

import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.application.PriceNegotiationStatus;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 신청 상세. 신청자와 공간 제공자가 같은 화면을 본다. */
public record ApplicationResponse(
        @Schema(description = "신청 ID", example = "1") Long id,
        @Schema(description = "신청 상태", example = "PENDING") ApplicationStatus status,
        @Schema(description = "거절 · 취소 · 마감 사유") String statusReason,
        @Schema(description = "신청한 공간") SpaceInfo space,
        @Schema(description = "신청자") ApplicantInfo applicant,
        @Schema(description = "활동 종류", example = "POPUP_STORE") ActivityType activityType,
        @Schema(description = "활동 이름") String activityTitle,
        @Schema(description = "활동 계획") String plan,
        @Schema(description = "예상 운영 인원", example = "3") int headcount,
        @Schema(description = "이용 시작일") LocalDate startDate,
        @Schema(description = "이용 종료일") LocalDate endDate,
        @Schema(description = "이용 일수", example = "2") int days,
        @Schema(description = "신청 시점 1일 이용료(원)", example = "50000") int listDailyPrice,
        @Schema(description = "정가 합계(원)", example = "100000") int listTotalPrice,
        @Schema(description = "현재 합의 금액(원) — 협의 전에는 정가 합계", example = "100000") int totalPrice,
        @Schema(description = "가격 협의 상태", example = "NONE") PriceNegotiationStatus negotiationStatus,
        @Schema(description = "답변을 기다리는 제안 금액 (없으면 null)", example = "90000") Integer pendingOfferAmount,
        @Schema(description = "신청 시각") LocalDateTime createdAt,
        @Schema(description = "확정 · 거절 · 취소 · 마감 시각") LocalDateTime decidedAt
) {

    public static ApplicationResponse of(Application application, String thumbnailUrl) {
        return new ApplicationResponse(application.getId(), application.getStatus(), application.getStatusReason(),
                SpaceInfo.of(application.getSpace(), thumbnailUrl), ApplicantInfo.from(application.getApplicant()),
                application.getActivityType(), application.getActivityTitle(), application.getPlan(),
                application.getHeadcount(), application.getStartDate(), application.getEndDate(), application.days(),
                application.getListDailyPrice(), application.getListTotalPrice(), application.getTotalPrice(),
                application.getNegotiationStatus(), application.getPendingOfferAmount(), application.getCreatedAt(),
                application.getDecidedAt());
    }

    public record SpaceInfo(
            @Schema(description = "공간 ID", example = "1") Long id,
            @Schema(description = "공간 이름") String title,
            @Schema(description = "서비스 지역", example = "JEONGWANG") Region region,
            @Schema(description = "도로명 주소") String address,
            @Schema(description = "대표 사진 주소 (없으면 null)") String thumbnailUrl
    ) {

        public static SpaceInfo of(Space space, String thumbnailUrl) {
            return new SpaceInfo(space.getId(), space.getTitle(), space.getRegion(), space.getAddress(), thumbnailUrl);
        }
    }

    public record ApplicantInfo(
            @Schema(description = "신청자 회원 ID", example = "1") Long id,
            @Schema(description = "신청자 이름", example = "이이용") String name
    ) {

        public static ApplicantInfo from(Member applicant) {
            return new ApplicantInfo(applicant.getId(), applicant.getName());
        }
    }
}
