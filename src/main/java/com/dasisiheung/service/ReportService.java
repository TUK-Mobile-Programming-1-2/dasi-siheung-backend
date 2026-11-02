package com.dasisiheung.service;

import com.dasisiheung.dto.report.ReportRequest;
import com.dasisiheung.dto.report.ReportResponse;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.report.Report;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원의 신고 접수. */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ReportTargetResolver targetResolver;

    /**
     * @throws BusinessException 대상이 없으면 REPORT_TARGET_NOT_FOUND, 자기 자신이면 CANNOT_REPORT_SELF,
     *                           처리 대기 중인 같은 신고가 있으면 DUPLICATE_REPORT
     */
    public ReportResponse report(Long reporterId, ReportRequest request) {
        ReportTarget target = targetResolver.find(request.targetType(), request.targetId());
        Member reporter = memberRepository.getReferenceById(reporterId);
        Report report = new Report(reporter, request.targetType(), request.targetId(), target.owner().getId(),
                request.reason(), request.detail());
        boolean duplicated = reportRepository.existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(reporterId,
                request.targetType(), request.targetId(), ReportStatus.PENDING);
        if (duplicated) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }
        return ReportResponse.from(reportRepository.saveAndFlush(report));
    }
}
