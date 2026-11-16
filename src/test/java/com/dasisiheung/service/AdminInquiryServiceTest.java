package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import com.dasisiheung.dto.inquiry.AdminInquiryResponse;
import com.dasisiheung.dto.inquiry.AnswerInquiryRequest;
import com.dasisiheung.entity.inquiry.Inquiry;
import com.dasisiheung.entity.inquiry.InquiryCategory;
import com.dasisiheung.entity.inquiry.InquiryStatus;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.InquiryPhotoRepository;
import com.dasisiheung.repository.InquiryRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminInquiryServiceTest {

    private static final long OWNER_ID = 1L;
    private static final long ADMIN_ID = 3L;
    private static final long INQUIRY_ID = 12L;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private InquiryPhotoRepository photoRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private InquiryPhotoUrls photoUrls;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AdminInquiryService service;
    private Inquiry inquiry;

    @BeforeEach
    void setUp() {
        service = new AdminInquiryService(inquiryRepository, photoRepository, memberRepository, photoUrls, eventPublisher,
                FixedClockConfig.fixedClock());
        inquiry = new Inquiry(ApplicationFixtures.applicant(OWNER_ID), InquiryCategory.ACCOUNT, "로그인이 안 돼요.");
        ReflectionTestUtils.setField(inquiry, "id", INQUIRY_ID);
    }

    @Test
    @DisplayName("답변하면 답변 완료가 되고, 문의한 회원에게 보낼 답변 이벤트를 발행한다")
    void answer() {
        given(inquiryRepository.getInquiryWithLock(INQUIRY_ID)).willReturn(inquiry);
        given(memberRepository.getReferenceById(ADMIN_ID)).willReturn(ApplicationFixtures.applicant(ADMIN_ID));
        given(photoUrls.of(INQUIRY_ID)).willReturn(List.of());

        AdminInquiryResponse response = service.answer(ADMIN_ID, INQUIRY_ID, new AnswerInquiryRequest("앱을 다시 실행해 주세요."));

        assertThat(response.status()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(response.answer()).isEqualTo("앱을 다시 실행해 주세요.");
        assertThat(response.answeredAt()).isEqualTo(FixedClockConfig.FIXED_LOCAL_NOW);
        assertThat(response.memberId()).isEqualTo(OWNER_ID);
        then(eventPublisher).should().publishEvent(new InquiryAnsweredEvent(INQUIRY_ID, OWNER_ID, InquiryCategory.ACCOUNT));
    }

    @Test
    @DisplayName("이미 답변한 문의는 409 INQUIRY_ALREADY_ANSWERED, 푸시 이벤트도 다시 보내지 않는다")
    void answerTwice() {
        inquiry.answer(ApplicationFixtures.applicant(ADMIN_ID), "먼저 한 답변", FixedClockConfig.FIXED_LOCAL_NOW);
        given(inquiryRepository.getInquiryWithLock(INQUIRY_ID)).willReturn(inquiry);
        given(memberRepository.getReferenceById(ADMIN_ID)).willReturn(ApplicationFixtures.applicant(ADMIN_ID));

        assertThatThrownBy(() -> service.answer(ADMIN_ID, INQUIRY_ID, new AnswerInquiryRequest("두 번째")))
                .extracting("errorCode").isEqualTo(ErrorCode.INQUIRY_ALREADY_ANSWERED);
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
        assertThat(inquiry.getAnswer()).isEqualTo("먼저 한 답변");
    }
}
