package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.report.MyReportResponse;
import com.dasisiheung.dto.report.ReportPhotoResponse;
import com.dasisiheung.dto.report.ReportRequest;
import com.dasisiheung.dto.report.ReportResponse;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.report.Report;
import com.dasisiheung.entity.report.ReportPhoto;
import com.dasisiheung.entity.report.ReportPhoto.StoredPhotoFile;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * 회원의 신고 접수. 증거 사진은 비공개 저장소에 두고 관리자만 임시 주소로 본다.
 * 사진 하나라도 실패하면 신고 전체가 롤백되고, 이미 올린 파일은 FileUploadService 가 지운다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private static final String PHOTO_DIRECTORY = "reports/%d/photos";

    private final ReportRepository reportRepository;
    private final ReportPhotoRepository reportPhotoRepository;
    private final MemberRepository memberRepository;
    private final ReportTargetResolver targetResolver;
    private final FileUploadService fileUploadService;
    private final Clock clock;

    /**
     * @throws BusinessException 사진이 5장을 넘으면 REPORT_PHOTO_LIMIT_EXCEEDED, 대상이 없으면 REPORT_TARGET_NOT_FOUND,
     *                           자기 자신이면 CANNOT_REPORT_SELF, 처리 대기 중인 같은 신고가 있으면 DUPLICATE_REPORT,
     *                           사진 형식 · 크기가 맞지 않으면 UNSUPPORTED_FILE_TYPE · FILE_TOO_LARGE
     */
    public ReportResponse report(Long reporterId, ReportRequest request) {
        List<MultipartFile> files = request.attachedPhotos();
        Report.validatePhotoCount(files.size());

        ReportTarget target = targetResolver.find(request.targetType(), request.targetId());
        Member reporter = memberRepository.getReferenceById(reporterId);
        Report report = new Report(reporter, request.targetType(), request.targetId(), target.owner().getId(),
                request.reason(), request.detail());
        boolean duplicated = reportRepository.existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(reporterId,
                request.targetType(), request.targetId(), ReportStatus.PENDING);
        if (duplicated) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }
        Report saved = reportRepository.saveAndFlush(report);

        List<ReportPhoto> savedPhotos = savePhotos(saved, files);
        return ReportResponse.of(saved, toResponses(savedPhotos));
    }

    /**
     * 내가 한 신고, 최근 순.
     *
     * @param status null 이면 전체
     */
    @Transactional(readOnly = true)
    public PageResponse<MyReportResponse> getMyReports(Long reporterId, ReportStatus status, PageQuery pageQuery) {
        Page<Report> reports = reportRepository.findMine(reporterId, status,
                pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        Map<Long, Long> photoCounts = reportPhotoRepository.countByReportIds(reports.map(Report::getId).getContent());
        return PageResponse.from(reports, report -> MyReportResponse.of(report,
                targetResolver.summaryOf(report.getTargetType(), report.getTargetId()),
                photoCounts.getOrDefault(report.getId(), 0L).intValue()));
    }

    private List<ReportPhoto> savePhotos(Report report, List<MultipartFile> files) {
        String directory = PHOTO_DIRECTORY.formatted(report.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        List<ReportPhoto> photos = files.stream().map(file -> {
            StoredFile stored = fileUploadService.upload(UploadPolicy.REPORT_PHOTO, directory, file);
            return new ReportPhoto(report, new StoredPhotoFile(stored.path(), stored.originalName(),
                    stored.type().contentType(), stored.size()), now);
        }).toList();
        return reportPhotoRepository.saveAllAndFlush(photos);
    }

    private List<ReportPhotoResponse> toResponses(List<ReportPhoto> photos) {
        return photos.stream()
                .map(photo -> ReportPhotoResponse.of(photo,
                        fileUploadService.url(UploadPolicy.REPORT_PHOTO, photo.getStoragePath())))
                .toList();
    }
}
