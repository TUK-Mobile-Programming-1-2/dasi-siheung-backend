package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.dasisiheung.dto.privacy.PurgePreviewResponse;
import com.dasisiheung.dto.privacy.PurgeResultResponse;
import com.dasisiheung.entity.privacy.PrivacyPurgeLog;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MonthlyUsageStatRepository;
import com.dasisiheung.repository.PrivacyRepository;
import com.dasisiheung.support.FixedClockConfig;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivacyServiceTest {

    private static final LocalDate TODAY = FixedClockConfig.FIXED_TODAY;

    @Mock
    private PrivacyRepository privacyRepository;

    @Mock
    private MonthlyUsageStatRepository statRepository;

    @Mock
    private FileUploadService fileUploadService;

    private PrivacyService privacyService;

    @BeforeEach
    void setUp() {
        privacyService = new PrivacyService(privacyRepository, statRepository, fileUploadService,
                new PrivacyProperties(null, 180, null), FixedClockConfig.fixedClock());
    }

    @Test
    @DisplayName("보관 기간 기본값 365 · 365 · 90일, 설정하면 그 값으로 기준일을 정한다")
    void cutoffs() {
        given(privacyRepository.findActivityPhotoPathsEndedBefore(TODAY.minusDays(90))).willReturn(List.of("a.jpg"));

        PurgePreviewResponse preview = privacyService.preview();

        assertThat(preview.applicationCutoff()).isEqualTo(TODAY.minusDays(365));
        assertThat(preview.conversationCutoff()).isEqualTo(TODAY.minusDays(180).atStartOfDay());
        assertThat(preview.activityPhotoCutoff()).isEqualTo(TODAY.minusDays(90));
        assertThat(preview.activityPhotosToDelete()).isEqualTo(1);
    }

    @Test
    @DisplayName("파기: 통계 → 비식별화 → 대화 · 사진 삭제, 사진 파일은 커밋 뒤 지우고 기록을 남긴다")
    void purge() {
        given(privacyRepository.countBookingsIntoStats(TODAY)).willReturn(2L);
        given(privacyRepository.anonymizeApplicationsEndedBefore(any(), any())).willReturn(5L);
        given(privacyRepository.deleteConversationsInactiveSince(any())).willReturn(1);
        given(privacyRepository.findActivityPhotoPathsEndedBefore(any())).willReturn(List.of("a.jpg", "b.jpg"));
        given(privacyRepository.deleteActivityPhotosEndedBefore(any())).willReturn(2);
        given(privacyRepository.save(any(PrivacyPurgeLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        PurgeResultResponse result = privacyService.purge(3L);

        assertThat(result.executedBy()).isEqualTo(3L);
        assertThat(result.countedBookings()).isEqualTo(2);
        assertThat(result.anonymizedApplications()).isEqualTo(5);
        assertThat(result.deletedActivityPhotos()).isEqualTo(2);
        then(fileUploadService).should().deleteAfterCommit(UploadPolicy.ACTIVITY_PHOTO, "a.jpg");
        then(fileUploadService).should().deleteAfterCommit(UploadPolicy.ACTIVITY_PHOTO, "b.jpg");
    }

    @Test
    @DisplayName("통계 조회 기간: 끝 월이 앞서거나 36개월을 넘으면 INVALID_INPUT")
    void statisticsRange() {
        assertThatThrownBy(() -> privacyService.getMonthlyStatistics(YearMonth.of(2026, 5), YearMonth.of(2026, 4)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> privacyService.getMonthlyStatistics(YearMonth.of(2023, 1), YearMonth.of(2026, 1)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(privacyService.getMonthlyStatistics(YearMonth.of(2024, 2), YearMonth.of(2027, 1)).rows()).isEmpty();
    }
}
