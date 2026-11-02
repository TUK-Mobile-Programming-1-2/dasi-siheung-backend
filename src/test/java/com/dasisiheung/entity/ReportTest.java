package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.report.Report;
import com.dasisiheung.entity.report.ReportAction;
import com.dasisiheung.entity.report.ReportReason;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.entity.report.ReportTargetType;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportTest {

    private final Member reporter = ApplicationFixtures.applicant(1L);
    private final Member admin = ApplicationFixtures.applicant(3L);

    @Test
    @DisplayName("자기 자신 · 자기 글은 신고할 수 없다")
    void cannotReportSelf() {
        assertThatThrownBy(() -> new Report(reporter, ReportTargetType.POST, 5L, 1L, ReportReason.SPAM, null))
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_REPORT_SELF);
    }

    @Test
    @DisplayName("처리하면 상태 · 처리 내용이 남고 다시 처리할 수 없다. 기각은 DISMISSED")
    void resolve() {
        Report report = new Report(reporter, ReportTargetType.SPACE, 10L, 2L, ReportReason.FRAUD, " 허위 사진 ");
        assertThat(report.getDetail()).isEqualTo("허위 사진");

        report.resolve(admin, ReportAction.HIDE_AND_SUSPEND, "1주 정지", FixedClockConfig.FIXED_LOCAL_NOW);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(report.getAction()).isEqualTo(ReportAction.HIDE_AND_SUSPEND);
        assertThatThrownBy(() -> report.resolve(admin, ReportAction.DISMISS, null, FixedClockConfig.FIXED_LOCAL_NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_ALREADY_RESOLVED);

        Report dismissed = new Report(reporter, ReportTargetType.POST, 5L, 2L, ReportReason.SPAM, null);
        dismissed.resolve(admin, ReportAction.DISMISS, null, FixedClockConfig.FIXED_LOCAL_NOW);
        assertThat(dismissed.getStatus()).isEqualTo(ReportStatus.DISMISSED);
    }

    @Test
    @DisplayName("회원 신고는 숨길 수 없다")
    void memberCannotBeHidden() {
        Report report = new Report(reporter, ReportTargetType.MEMBER, 2L, 2L, ReportReason.ABUSIVE, null);

        assertThatThrownBy(() -> report.resolve(admin, ReportAction.HIDE, null, FixedClockConfig.FIXED_LOCAL_NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REPORT_ACTION);
    }

    @Test
    @DisplayName("관리자 · 탈퇴 회원은 정지할 수 없다")
    void suspendRules() {
        Member target = ApplicationFixtures.applicant(2L);
        target.suspend(FixedClockConfig.FIXED_LOCAL_NOW.plusDays(7));
        assertThat(target.isActive()).isFalse();

        target.withdraw(FixedClockConfig.FIXED_LOCAL_NOW);
        assertThatThrownBy(() -> target.suspend(FixedClockConfig.FIXED_LOCAL_NOW.plusDays(7)))
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_SUSPEND_MEMBER);
    }
}
