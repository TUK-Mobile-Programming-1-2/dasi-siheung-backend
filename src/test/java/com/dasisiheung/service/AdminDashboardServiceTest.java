package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.dasisiheung.dto.admin.DashboardResponse;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.member.MemberStatus;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.ReportRepository;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.repository.projection.EnumCount;
import com.dasisiheung.support.FixedClockConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ReportRepository reportRepository;

    @Test
    @DisplayName("집계에 없는 상태·지역·역할은 0 으로 채우고 선언 순서를 지킨다")
    void zeroFilledCounts() {
        given(spaceRepository.countByStatus()).willReturn(List.of(
                new EnumCount<>(SpaceStatus.PUBLISHED, 4L), new EnumCount<>(SpaceStatus.PENDING_REVIEW, 2L)));
        given(spaceRepository.countByRegion(SpaceStatus.PUBLISHED)).willReturn(List.of(new EnumCount<>(Region.BAEGOT, 4L)));
        given(memberRepository.countByRoleExcludingStatus(MemberStatus.WITHDRAWN))
                .willReturn(List.of(new EnumCount<>(Role.USER, 10L)));
        given(spaceRepository.findTop5ByOrderByCreatedAtDesc()).willReturn(List.of());
        given(reportRepository.countByStatus(ReportStatus.PENDING)).willReturn(1L);
        given(applicationRepository.countByStatus())
                .willReturn(List.of(new EnumCount<>(ApplicationStatus.PENDING, 5L)));

        DashboardResponse dashboard = new AdminDashboardService(spaceRepository, memberRepository,
                applicationRepository, reportRepository, FixedClockConfig.fixedClock()).getDashboard();

        assertThat(dashboard.pendingReviewCount()).isEqualTo(2);
        assertThat(dashboard.spacesByStatus()).containsOnlyKeys(SpaceStatus.values());
        assertThat(dashboard.spacesByStatus().keySet()).first().isEqualTo(SpaceStatus.PENDING_REVIEW);
        assertThat(dashboard.spacesByStatus().get(SpaceStatus.HIDDEN)).isZero();
        assertThat(dashboard.publishedSpacesByRegion()).containsEntry(Region.BAEGOT, 4L).containsEntry(Region.JEONGWANG, 0L);
        assertThat(dashboard.membersByRole()).containsEntry(Role.USER, 10L).containsEntry(Role.ADMIN, 0L);
        assertThat(dashboard.pendingApplicationCount()).isEqualTo(5);
        assertThat(dashboard.applicationsByStatus()).containsOnlyKeys(ApplicationStatus.values())
                .containsEntry(ApplicationStatus.APPROVED, 0L);
        assertThat(dashboard.pendingReportCount()).isEqualTo(1);
        assertThat(dashboard.generatedAt()).isEqualTo(FixedClockConfig.FIXED_LOCAL_NOW);
    }
}
