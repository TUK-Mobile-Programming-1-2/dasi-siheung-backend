package com.dasisiheung.support;

import com.dasisiheung.dto.application.ApplicationRequest;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationPeriod;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import java.time.LocalDate;
import org.springframework.test.util.ReflectionTestUtils;

/** 신청 테스트 데이터. */
public final class ApplicationFixtures {

    private ApplicationFixtures() {
    }

    public static ApplicationRequest request(long spaceId, LocalDate startDate, LocalDate endDate) {
        return new ApplicationRequest(spaceId, ActivityType.POPUP_STORE, "수제 캔들 팝업 스토어",
                "주말 이틀 동안 수제 캔들을 전시 · 판매합니다.", 3, startDate, endDate);
    }

    public static Member applicant(long id) {
        Member applicant = Member.signUp("user" + id + "@example.com", "hash", "이이용", "01011112222", Role.USER);
        ReflectionTestUtils.setField(applicant, "id", id);
        return applicant;
    }

    /** 공개 상태로 바꾼 공간. */
    public static Space publishedSpace(Space space) {
        ReflectionTestUtils.setField(space, "status", SpaceStatus.PUBLISHED);
        return space;
    }

    /** 저장된 것처럼 ID · 시각을 채운 검토 중 신청. */
    public static Application application(long id, Space space, Member applicant, LocalDate startDate,
                                          LocalDate endDate, LocalDate today) {
        Application application = Application.submit(space, applicant,
                request(space.getId(), startDate, endDate).toActivityDetails(),
                new ApplicationPeriod(startDate, endDate), today);
        ReflectionTestUtils.setField(application, "id", id);
        ReflectionTestUtils.setField(application, "version", 0L);
        ReflectionTestUtils.setField(application, "createdAt", FixedClockConfig.FIXED_LOCAL_NOW);
        ReflectionTestUtils.setField(application, "updatedAt", FixedClockConfig.FIXED_LOCAL_NOW);
        return application;
    }
}
