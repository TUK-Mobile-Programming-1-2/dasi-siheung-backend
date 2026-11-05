package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.report.AdminReportResponse;
import com.dasisiheung.dto.report.AdminReportResponse.TargetView;
import com.dasisiheung.dto.report.MemberSuspensionResponse;
import com.dasisiheung.dto.report.ReportPhotoResponse;
import com.dasisiheung.dto.report.ResolveReportRequest;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.report.Report;
import com.dasisiheung.entity.report.ReportAction;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.entity.report.ReportTargetType;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.RefreshTokenRepository;
import com.dasisiheung.repository.ReportPhotoRepository;
import com.dasisiheung.repository.ReportRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 신고 처리 · 이용 정지.
 * 한 신고를 처리하면 같은 대상의 처리 대기 신고도 같은 결과로 함께 닫는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final ReportPhotoRepository reportPhotoRepository;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ReportTargetResolver targetResolver;
    private final FileUploadService fileUploadService;
    private final Clock clock;

    /** 처리 대기는 오래된 순, 그 밖은 최근 순. */
    public PageResponse<AdminReportResponse> getReports(ReportStatus status, ReportTargetType targetType,
                                                        PageQuery pageQuery) {
        Sort.Direction direction = status == ReportStatus.PENDING ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<Report> reports = reportRepository.search(status, targetType,
                pageQuery.toPageable(Sort.by(direction, "createdAt", "id")));
        Map<Long, Long> photoCounts = reportPhotoRepository.countByReportIds(
                reports.map(Report::getId).getContent());
        return PageResponse.from(reports, report -> AdminReportResponse.summary(report, targetOf(report),
                photoCounts.getOrDefault(report.getId(), 0L).intValue()));
    }

    /** 증거 사진 임시 주소(10분)를 함께 준다. */
    public AdminReportResponse getReport(Long reportId) {
        Report report = reportRepository.getReport(reportId);
        List<ReportPhotoResponse> photos = reportPhotoRepository.findByReport_IdOrderByIdAsc(reportId).stream()
                .map(photo -> ReportPhotoResponse.of(photo,
                        fileUploadService.url(UploadPolicy.REPORT_PHOTO, photo.getStoragePath())))
                .toList();
        return AdminReportResponse.detail(report, targetOf(report), photos);
    }

    /**
     * 신고를 처리한다. 순서: 신고 닫기 → 이용 정지 → 대상 숨김 (숨김의 집계 갱신이 영속성 컨텍스트를 비우므로 마지막).
     */
    @Transactional
    public AdminReportResponse resolve(Long adminId, Long reportId, ResolveReportRequest request) {
        Report report = reportRepository.getReport(reportId);
        Member admin = memberRepository.getReferenceById(adminId);
        LocalDateTime now = LocalDateTime.now(clock);
        ReportAction action = request.action();

        List<Report> sameTarget = reportRepository.findByTargetTypeAndTargetIdAndStatus(report.getTargetType(),
                report.getTargetId(), ReportStatus.PENDING);
        report.resolve(admin, action, request.note(), now);
        sameTarget.stream().filter(other -> !other.getId().equals(reportId))
                .forEach(other -> other.resolve(admin, action, request.note(), now));

        ReportTarget target = action.isHidesTarget() || action.isSuspendsOwner()
                ? targetResolver.find(report.getTargetType(), report.getTargetId()) : null;
        if (action.isSuspendsOwner()) {
            suspend(target.owner(), request.suspendDays(), now);
        }
        reportRepository.flush();
        if (action.isHidesTarget()) {
            targetResolver.hide(report.getTargetType(), report.getTargetId(), hiddenReason(request.note()), now);
        }
        return getReport(reportId);
    }

    @Transactional
    public MemberSuspensionResponse suspend(Long memberId, int days) {
        Member member = getMember(memberId);
        suspend(member, days, LocalDateTime.now(clock));
        return MemberSuspensionResponse.from(member);
    }

    @Transactional
    public MemberSuspensionResponse unsuspend(Long memberId) {
        Member member = getMember(memberId);
        member.liftSuspension();
        return MemberSuspensionResponse.from(member);
    }

    /** 정지하고 리프레시 토큰을 지워 다시 로그인하지 못하게 한다. (발급된 액세스 토큰은 최대 30분 유효) */
    private void suspend(Member member, int days, LocalDateTime now) {
        member.suspend(now.plusDays(days));
        refreshTokenRepository.deleteAllByMemberId(member.getId());
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private static String hiddenReason(String note) {
        return note == null || note.isBlank() ? "신고 처리로 숨김 처리되었습니다." : note.strip();
    }

    /** @return 대상 요약, 대상이 지워졌으면 null */
    private TargetView targetOf(Report report) {
        try {
            ReportTarget found = targetResolver.find(report.getTargetType(), report.getTargetId());
            return new TargetView(found.summary(), found.visible(), found.owner().getId(), found.owner().getName(),
                    found.owner().getStatus());
        } catch (BusinessException deleted) {
            return null;
        }
    }
}
