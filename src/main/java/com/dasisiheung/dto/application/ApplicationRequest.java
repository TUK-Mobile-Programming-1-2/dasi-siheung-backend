package com.dasisiheung.dto.application;

import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationPeriod;
import com.dasisiheung.entity.space.ActivityType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 공간 이용 신청서. */
public record ApplicationRequest(
        @Schema(description = "신청할 공간 ID", example = "1")
        @NotNull Long spaceId,

        @Schema(description = "활동 종류 (공간이 허용한 활동만)", example = "POPUP_STORE")
        @NotNull ActivityType activityType,

        @Schema(description = "활동 이름 (최대 60자)", example = "수제 캔들 팝업 스토어")
        @NotBlank @Size(max = 60) String activityTitle,

        @Schema(description = "활동 계획 · 공간 사용 방법 (최대 2000자)", example = "주말 이틀 동안 수제 캔들을 전시 · 판매합니다.")
        @NotBlank @Size(max = 2000) String plan,

        @Schema(description = "예상 운영 인원 (1~200)", example = "3")
        @NotNull @Min(1) @Max(200) Integer headcount,

        @Schema(description = "이용 시작일 yyyy-MM-dd (오늘부터)", example = "2026-10-24")
        @NotNull LocalDate startDate,

        @Schema(description = "이용 종료일 yyyy-MM-dd (최대 30일)", example = "2026-10-25")
        @NotNull LocalDate endDate,

        @Schema(description = "처음부터 제안할 금액(원, 기간 합계, 선택) — 없으면 정가로 신청", example = "90000")
        @Min(1) Integer offerAmount,

        @Schema(description = "가격 제안 한마디 (선택, 최대 300자)")
        @Size(max = 300) String offerMessage
) {

    public boolean hasOffer() {
        return offerAmount != null;
    }


    @JsonIgnore
    @AssertTrue(message = "종료일은 시작일보다 빠를 수 없습니다.")
    public boolean isPeriodOrdered() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    public Application.ActivityDetails toActivityDetails() {
        return new Application.ActivityDetails(activityType, activityTitle, plan, headcount);
    }

    public ApplicationPeriod toPeriod() {
        return new ApplicationPeriod(startDate, endDate);
    }
}
