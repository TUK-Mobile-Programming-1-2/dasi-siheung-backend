package com.dasisiheung.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 확정 예약을 만들고, 필요하면 이용 기간을 과거로 옮겨 "이용이 끝난 예약"을 만든다.
 * (API 로는 지난 날짜를 신청할 수 없어 기간만 SQL 로 옮긴다)
 */
@TestComponent
@RequiredArgsConstructor
public class BookingScenario {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final SpaceScenario spaceScenario;
    private final ApplicationScenario applicationScenario;

    /** 공개 공간 → 신청 → 승인까지 마친 확정 예약 ID. */
    public long approvedBooking(TestAccounts.Account provider, TestAccounts.Account admin,
                                TestAccounts.Account applicant, LocalDate startDate, LocalDate endDate)
            throws Exception {
        long spaceId = spaceScenario.publishedSpace(provider, admin, startDate.datesUntil(endDate.plusDays(1))
                .collect(Collectors.toSet()));
        long applicationId = applicationScenario.submit(applicant, spaceId, startDate, endDate);
        mockMvc.perform(post("/api/v1/provider/applications/{id}/approve", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk());
        return applicationId;
    }

    /** 이용 기간을 오늘 기준으로 옮긴다 (예: -3, -2 → 사흘 전~이틀 전). 영속성 컨텍스트도 비운다. */
    public void movePeriod(long applicationId, int startOffsetDays, int endOffsetDays) {
        entityManager.flush();
        jdbcTemplate.update("UPDATE space_application SET start_date = current_date + ?, end_date = current_date + ? "
                + "WHERE id = ?", startOffsetDays, endOffsetDays, applicationId);
        entityManager.clear();
    }
}
