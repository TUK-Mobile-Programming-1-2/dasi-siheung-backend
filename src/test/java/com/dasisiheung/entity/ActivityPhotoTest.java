package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.application.ActivityPhoto;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.PhotoPhase;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActivityPhotoTest {

    private static final LocalDate TODAY = FixedClockConfig.FIXED_TODAY;
    private static final LocalDate START = TODAY.plusDays(2);
    private static final LocalDate END = START.plusDays(1);

    private Application application;

    @BeforeEach
    void setUp() {
        application = ApplicationFixtures.application(7L, ApplicationFixtures.publishedSpace(
                        SpaceFixtures.space(10L, SpaceFixtures.provider(2L), Set.of(START, END), TODAY)),
                ApplicationFixtures.applicant(1L), START, END, TODAY);
    }

    @Test
    @DisplayName("이용 전 사진은 시작 하루 전~종료일, 이용 후 사진은 시작일~종료 7일 뒤")
    void windows() {
        assertThat(PhotoPhase.BEFORE.isOpen(START, END, START.minusDays(1))).isTrue();
        assertThat(PhotoPhase.BEFORE.isOpen(START, END, START.minusDays(2))).isFalse();
        assertThat(PhotoPhase.BEFORE.isOpen(START, END, END.plusDays(1))).isFalse();
        assertThat(PhotoPhase.AFTER.isOpen(START, END, START.minusDays(1))).isFalse();
        assertThat(PhotoPhase.AFTER.isOpen(START, END, END.plusDays(7))).isTrue();
        assertThat(PhotoPhase.AFTER.isOpen(START, END, END.plusDays(8))).isFalse();
    }

    @Test
    @DisplayName("확정 예약 · 참여자 · 기간 안 · 시점별 10장까지만")
    void validateUpload() {
        assertThatThrownBy(() -> ActivityPhoto.validateUpload(application, 1L, PhotoPhase.BEFORE, 0, 1, START))
                .extracting("errorCode").isEqualTo(ErrorCode.ACTIVITY_PHOTO_NOT_ALLOWED);

        application.approve(2L, false, TODAY, FixedClockConfig.FIXED_LOCAL_NOW);
        assertThatCode(() -> ActivityPhoto.validateUpload(application, 1L, PhotoPhase.BEFORE, 0, 1, START))
                .doesNotThrowAnyException();
        assertThatCode(() -> ActivityPhoto.validateUpload(application, 2L, PhotoPhase.AFTER, 9, 1, END))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ActivityPhoto.validateUpload(application, 1L, PhotoPhase.AFTER, 9, 2, END))
                .extracting("errorCode").isEqualTo(ErrorCode.ACTIVITY_PHOTO_LIMIT_EXCEEDED);
        assertThatThrownBy(() -> ActivityPhoto.validateUpload(application, 99L, PhotoPhase.BEFORE, 0, 1, START))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_APPLICATION_PARTICIPANT);
        assertThatThrownBy(() -> ActivityPhoto.validateUpload(application, 1L, PhotoPhase.AFTER, 0, 1, TODAY))
                .extracting("errorCode").isEqualTo(ErrorCode.ACTIVITY_PHOTO_NOT_ALLOWED);
    }
}
