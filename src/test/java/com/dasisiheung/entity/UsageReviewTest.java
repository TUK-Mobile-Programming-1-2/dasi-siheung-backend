package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.review.UsageReview;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UsageReviewTest {

    private static final LocalDate TODAY = FixedClockConfig.FIXED_TODAY;
    private static final LocalDate START = TODAY.plusDays(1);
    private static final long USER_ID = 1L;
    private static final long PROVIDER_ID = 2L;

    private Application application;

    @BeforeEach
    void setUp() {
        Space space = ApplicationFixtures.publishedSpace(SpaceFixtures.space(10L, SpaceFixtures.provider(PROVIDER_ID),
                Set.of(START, START.plusDays(1)), TODAY));
        application = ApplicationFixtures.application(7L, space, ApplicationFixtures.applicant(USER_ID),
                START, START.plusDays(1), TODAY);
    }

    @Test
    @DisplayName("이용이 끝난(종료일 다음 날부터) 확정 예약의 신청자만 쓸 수 있다")
    void writeAfterUse() {
        application.approve(PROVIDER_ID, false, TODAY, FixedClockConfig.FIXED_LOCAL_NOW);
        LocalDate afterUse = START.plusDays(2);

        UsageReview review = UsageReview.write(application, USER_ID, 5, " 좋았어요 ", afterUse);

        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getContent()).isEqualTo("좋았어요");
        assertThat(review.getSpace().getId()).isEqualTo(10L);
        assertThatThrownBy(() -> UsageReview.write(application, USER_ID, 5, "아직 이용 중", START.plusDays(1)))
                .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
        assertThatThrownBy(() -> UsageReview.write(application, 99L, 5, "남의 예약", afterUse))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_APPLICATION_PARTICIPANT);
        assertThatThrownBy(() -> UsageReview.write(application, USER_ID, 6, "평점 범위", afterUse))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("확정되지 않은 신청에는 쓸 수 없다")
    void notApproved() {
        assertThatThrownBy(() -> UsageReview.write(application, USER_ID, 4, "검토 중", START.plusDays(5)))
                .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
    }

    @Test
    @DisplayName("수정하면 평점 변화량을 돌려주고, 답글은 공간 제공자만")
    void editAndReply() {
        application.approve(PROVIDER_ID, false, TODAY, FixedClockConfig.FIXED_LOCAL_NOW);
        UsageReview review = UsageReview.write(application, USER_ID, 5, "좋았어요", START.plusDays(2));

        assertThat(review.edit(USER_ID, 3, "조금 아쉬웠어요")).isEqualTo(-2);
        assertThatThrownBy(() -> review.edit(99L, 1, "남의 후기"))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_REVIEW_OWNER);

        review.reply(PROVIDER_ID, " 감사합니다 ", FixedClockConfig.FIXED_LOCAL_NOW);
        assertThat(review.getReply()).isEqualTo("감사합니다");
        assertThatThrownBy(() -> review.reply(99L, "남의 공간", FixedClockConfig.FIXED_LOCAL_NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_SPACE_OWNER);
    }
}
