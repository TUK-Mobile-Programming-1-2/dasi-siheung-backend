package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final LocalDate TODAY = FixedClockConfig.FIXED_TODAY;
    private static final LocalDate START = TODAY.plusDays(1);
    private static final long USER_ID = 1L;
    private static final long PROVIDER_ID = 2L;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private MemberRepository memberRepository;

    private ApplicationService applicationService;
    private Space space;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(applicationRepository, spaceRepository, memberRepository,
                new ApplicationResponseMapper(new SpaceResponseMapper(mock(FileUploadService.class))),
                FixedClockConfig.fixedClock());
        space = ApplicationFixtures.publishedSpace(
                SpaceFixtures.space(10L, SpaceFixtures.provider(PROVIDER_ID), Set.of(START, START.plusDays(1)), TODAY));
    }

    @Test
    @DisplayName("신청서를 저장하고 상세를 돌려준다")
    void submit() {
        given(memberRepository.findById(USER_ID)).willReturn(Optional.of(ApplicationFixtures.applicant(USER_ID)));
        given(spaceRepository.getSpace(10L)).willReturn(space);
        given(applicationRepository.save(any(Application.class))).willAnswer(invocation -> invocation.getArgument(0));

        ApplicationResponse response = applicationService.submit(USER_ID, ApplicationFixtures.request(10L, START, START));

        assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(response.totalPrice()).isEqualTo(50_000);
        assertThat(response.space().id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("같은 공간 · 겹치는 기간에 검토 중인 내 신청이 있으면 DUPLICATE_APPLICATION 이고 저장하지 않는다")
    void duplicate() {
        given(memberRepository.findById(USER_ID)).willReturn(Optional.of(ApplicationFixtures.applicant(USER_ID)));
        given(spaceRepository.getSpace(10L)).willReturn(space);
        given(applicationRepository.existsOverlappingByApplicant(10L, USER_ID, ApplicationStatus.PENDING, START, START))
                .willReturn(true);

        assertThatThrownBy(() -> applicationService.submit(USER_ID, ApplicationFixtures.request(10L, START, START)))
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_APPLICATION);
        then(applicationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("신청 상세는 신청자 · 공간 제공자만, 다른 회원은 NOT_APPLICATION_PARTICIPANT")
    void participantOnly() {
        Application application = ApplicationFixtures.application(7L, space, ApplicationFixtures.applicant(USER_ID),
                START, START, TODAY);
        given(applicationRepository.getApplication(7L)).willReturn(application);

        assertThat(applicationService.getApplication(USER_ID, 7L).id()).isEqualTo(7L);
        assertThat(applicationService.getApplication(PROVIDER_ID, 7L).id()).isEqualTo(7L);
        assertThatThrownBy(() -> applicationService.getApplication(99L, 7L))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_APPLICATION_PARTICIPANT);
    }
}
