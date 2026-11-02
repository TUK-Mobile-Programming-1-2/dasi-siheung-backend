package com.dasisiheung.service;

import com.dasisiheung.dto.admin.DashboardResponse;
import com.dasisiheung.dto.admin.DashboardResponse.RecentSpace;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 대시보드 집계. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final SpaceRepository spaceRepository;
    private final MemberRepository memberRepository;
    private final ApplicationRepository applicationRepository;
    private final ReportRepository reportRepository;
    private final Clock clock;

    public DashboardResponse getDashboard() {
        Map<SpaceStatus, Long> spacesByStatus = zeroFilled(SpaceStatus.class, spaceRepository.countByStatus());
        Map<ApplicationStatus, Long> applicationsByStatus =
                zeroFilled(ApplicationStatus.class, applicationRepository.countByStatus());
        return new DashboardResponse(
                spacesByStatus.get(SpaceStatus.PENDING_REVIEW),
                spacesByStatus,
                zeroFilled(Region.class, spaceRepository.countByRegion(SpaceStatus.PUBLISHED)),
                zeroFilled(Role.class, memberRepository.countByRoleExcludingStatus(MemberStatus.WITHDRAWN)),
                applicationsByStatus.get(ApplicationStatus.PENDING),
                applicationsByStatus,
                reportRepository.countByStatus(ReportStatus.PENDING),
                spaceRepository.findTop5ByOrderByCreatedAtDesc().stream().map(RecentSpace::from).toList(),
                LocalDateTime.now(clock));
    }

    /** enum 선언 순서대로, 집계에 없는 항목은 0 으로 채운다. */
    static <E extends Enum<E>> Map<E, Long> zeroFilled(Class<E> type, List<EnumCount<E>> counts) {
        Map<E, Long> result = new LinkedHashMap<>();
        for (E key : type.getEnumConstants()) {
            result.put(key, 0L);
        }
        counts.forEach(count -> result.put(count.key(), count.count()));
        return result;
    }
}
