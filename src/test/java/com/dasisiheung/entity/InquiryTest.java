package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.dasisiheung.entity.inquiry.Inquiry;
import com.dasisiheung.entity.inquiry.InquiryCategory;
import com.dasisiheung.entity.inquiry.InquiryStatus;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InquiryTest {

    private final Member owner = ApplicationFixtures.applicant(1L);
    private final Member admin = ApplicationFixtures.applicant(3L);

    @Test
    @DisplayName("새 문의는 답변 대기이고, 내용 앞뒤 공백을 지운다")
    void create() {
        Inquiry inquiry = new Inquiry(owner, InquiryCategory.ACCOUNT, "  로그인이 안 돼요.\n ");

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.WAITING);
        assertThat(inquiry.getContent()).isEqualTo("로그인이 안 돼요.");
        assertThat(inquiry.getAnswer()).isNull();
        assertThat(inquiry.getAnsweredAt()).isNull();
    }

    @Test
    @DisplayName("본인 문의만 볼 수 있다")
    void validateOwner() {
        Inquiry inquiry = new Inquiry(owner, InquiryCategory.ETC, "문의");

        assertThatCode(() -> inquiry.validateOwner(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> inquiry.validateOwner(2L))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_INQUIRY_OWNER);
    }

    @Test
    @DisplayName("답변하면 답변 · 시각 · 답변한 관리자가 남고, 두 번 답변할 수 없다")
    void answer() {
        Inquiry inquiry = new Inquiry(owner, InquiryCategory.PAYMENT, "환불 문의");

        inquiry.answer(admin, " 공간 제공자와 대화로 정해 주세요. ", FixedClockConfig.FIXED_LOCAL_NOW);

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(inquiry.isAnswered()).isTrue();
        assertThat(inquiry.getAnswer()).isEqualTo("공간 제공자와 대화로 정해 주세요.");
        assertThat(inquiry.getAnsweredAt()).isEqualTo(FixedClockConfig.FIXED_LOCAL_NOW);
        assertThat(inquiry.getAnsweredBy()).isSameAs(admin);
        assertThatThrownBy(() -> inquiry.answer(admin, "다시", FixedClockConfig.FIXED_LOCAL_NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.INQUIRY_ALREADY_ANSWERED);
    }

    @Test
    @DisplayName("사진은 이미 올린 것과 합쳐 3장까지")
    void photoLimit() {
        Inquiry inquiry = new Inquiry(owner, InquiryCategory.SPACE, "공간 정보가 달라요");

        assertThatCode(() -> inquiry.validatePhotoUpload(1L, 0, Inquiry.MAX_PHOTOS)).doesNotThrowAnyException();
        assertThatCode(() -> inquiry.validatePhotoUpload(1L, 2, 1)).doesNotThrowAnyException();
        assertThatThrownBy(() -> inquiry.validatePhotoUpload(1L, 2, 2))
                .extracting("errorCode").isEqualTo(ErrorCode.INQUIRY_PHOTO_LIMIT_EXCEEDED);
        assertThatThrownBy(() -> inquiry.validatePhotoUpload(1L, 0, Inquiry.MAX_PHOTOS + 1))
                .extracting("errorCode").isEqualTo(ErrorCode.INQUIRY_PHOTO_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("남의 문의 · 답변이 끝난 문의에는 사진을 올릴 수 없다")
    void photoUploadRules() {
        Inquiry inquiry = new Inquiry(owner, InquiryCategory.APPLICATION, "신청 문의");

        assertThatThrownBy(() -> inquiry.validatePhotoUpload(2L, 0, 1))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_INQUIRY_OWNER);

        inquiry.answer(admin, "답변", FixedClockConfig.FIXED_LOCAL_NOW);
        assertThatThrownBy(() -> inquiry.validatePhotoUpload(1L, 0, 1))
                .extracting("errorCode").isEqualTo(ErrorCode.INQUIRY_ALREADY_ANSWERED);
    }
}
