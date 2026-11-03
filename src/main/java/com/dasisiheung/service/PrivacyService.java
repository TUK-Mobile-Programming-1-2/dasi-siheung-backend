package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.privacy.MonthlyStatisticsResponse;
import com.dasisiheung.dto.privacy.PurgePreviewResponse;
import com.dasisiheung.dto.privacy.PurgeResultResponse;
import com.dasisiheung.entity.privacy.PrivacyPurgeLog;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MonthlyUsageStatRepository;
import com.dasisiheung.repository.PrivacyRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인정보 파기 · 비식별화와 통계 보존.
 * <ol>
 *   <li>이용이 끝난 확정 예약을 월별 통계에 더한다 (개인 정보 없이 수치만 남김)</li>
 *   <li>보관 기간이 지난 신청서 글 · 가격 제안 한마디를 비식별화한다 (날짜 · 금액은 정산 기록으로 남김)</li>
 *   <li>오래된 1:1 대화와 활동 사진을 지운다 (사진 파일은 커밋 뒤 저장소에서 삭제)</li>
 * </ol>
 * 매일 자동 실행되고, 관리자가 미리보기 후 바로 실행할 수도 있다. 여러 번 실행해도 결과가 같다.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(PrivacyProperties.class)
@Transactional(readOnly = true)
public class PrivacyService {

    /** 통계 조회는 최대 3년. */
    private static final int MAX_STAT_MONTHS = 36;

    private final PrivacyRepository privacyRepository;
    private final MonthlyUsageStatRepository statRepository;
    private final FileUploadService fileUploadService;
    private final PrivacyProperties properties;
    private final Clock clock;

    public PurgePreviewResponse preview() {
        LocalDate today = LocalDate.now(clock);
        Cutoffs cutoffs = cutoffs(today);
        return new PurgePreviewResponse(cutoffs.application(),
                privacyRepository.countApplicationsToAnonymize(cutoffs.application(), today),
                cutoffs.conversation(), privacyRepository.countConversationsInactiveSince(cutoffs.conversation()),
                cutoffs.activityPhoto(), privacyRepository.findActivityPhotoPathsEndedBefore(cutoffs.activityPhoto()).size());
    }

    /** @param executedBy 실행한 관리자 ID (자동 실행이면 null) */
    @Transactional
    public PurgeResultResponse purge(Long executedBy) {
        LocalDate today = LocalDate.now(clock);
        Cutoffs cutoffs = cutoffs(today);

        long counted = privacyRepository.countBookingsIntoStats(today);
        long anonymized = privacyRepository.anonymizeApplicationsEndedBefore(cutoffs.application(), LocalDateTime.now(clock));
        int conversations = privacyRepository.deleteConversationsInactiveSince(cutoffs.conversation());
        List<String> photoPaths = privacyRepository.findActivityPhotoPathsEndedBefore(cutoffs.activityPhoto());
        int photos = privacyRepository.deleteActivityPhotosEndedBefore(cutoffs.activityPhoto());
        photoPaths.forEach(path -> fileUploadService.deleteAfterCommit(UploadPolicy.ACTIVITY_PHOTO, path));

        PrivacyPurgeLog log = privacyRepository.save(new PrivacyPurgeLog(executedBy, cutoffs.application(),
                (int) counted, (int) anonymized, conversations, photos, LocalDateTime.now(clock)));
        return PurgeResultResponse.from(log);
    }

    /** 통계만 최신으로 (어제까지 끝난 확정 예약). */
    @Transactional
    public long refreshStatistics() {
        return privacyRepository.countBookingsIntoStats(LocalDate.now(clock));
    }

    public PageResponse<PurgeResultResponse> getLogs(PageQuery pageQuery) {
        return PageResponse.from(privacyRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "id"))),
                PurgeResultResponse::from);
    }

    /** @throws BusinessException 끝 월이 시작 월보다 빠르거나 36개월을 넘으면 INVALID_INPUT */
    public MonthlyStatisticsResponse getMonthlyStatistics(YearMonth from, YearMonth to) {
        if (to.isBefore(from) || from.plusMonths(MAX_STAT_MONTHS - 1).isBefore(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "조회 기간은 시작 월부터 최대 36개월입니다.");
        }
        return MonthlyStatisticsResponse.of(from, to, statRepository.findBetween(from.atDay(1), to.atDay(1)));
    }

    private Cutoffs cutoffs(LocalDate today) {
        return new Cutoffs(today.minusDays(properties.applicationRetentionDays()),
                today.minusDays(properties.conversationRetentionDays()).atStartOfDay(),
                today.minusDays(properties.activityPhotoRetentionDays()));
    }

    private record Cutoffs(LocalDate application, LocalDateTime conversation, LocalDate activityPhoto) {
    }
}
