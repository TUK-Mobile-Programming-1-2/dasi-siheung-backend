package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.ApplicationScenario;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 승인 → 예약 확정 → 겹치는 신청 자동 마감 → 이용자 화면 반영. */
@IntegrationTest
@Transactional
class BookingIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ClockConfig.SERVICE_ZONE);
    private static final LocalDate DAY1 = TODAY.plusDays(1);
    private static final LocalDate DAY2 = TODAY.plusDays(2);
    private static final LocalDate DAY3 = TODAY.plusDays(3);
    private static final LocalDate DAY4 = TODAY.plusDays(4);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Autowired
    private ApplicationScenario applicationScenario;

    @Test
    @DisplayName("승인하면 겹치는 신청은 마감, 안 겹치는 신청은 그대로, 확정 날짜는 상세 · 검색 · 새 신청에서 빠진다")
    void approveClosesOverlapping() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account userA = testAccounts.create(Role.USER);
        TestAccounts.Account userB = testAccounts.create(Role.USER);
        TestAccounts.Account userC = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN),
                Set.of(DAY1, DAY2, DAY3, DAY4));
        long approved = applicationScenario.submit(userA, spaceId, DAY1, DAY2);
        long overlapping = applicationScenario.submit(userB, spaceId, DAY2, DAY3);
        long separate = applicationScenario.submit(userC, spaceId, DAY4, DAY4);

        mockMvc.perform(post("/api/v1/provider/applications/{id}/approve", approved)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        expectStatus(userB, overlapping, "CLOSED");
        expectStatus(userC, separate, "PENDING");
        mockMvc.perform(post("/api/v1/provider/applications/{id}/approve", overlapping)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_APPLICATION_STATUS"));

        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(jsonPath("$.data.availableDates.length()").value(2))
                .andExpect(jsonPath("$.data.availableDates[0]").value(DAY3.toString()));

        mockMvc.perform(get("/api/v1/spaces").param("startDate", DAY1.toString()).param("endDate", DAY1.toString())
                        .param("size", "100"))
                .andExpect(jsonPath("$.data.content[?(@.id == %d)]".formatted(spaceId)).isEmpty());

        mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, testAccounts.create(Role.USER).bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationFixtures.request(spaceId, DAY2, DAY2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_ALREADY_BOOKED"));
    }

    @Test
    @DisplayName("가격 협의 대기 중인 신청은 승인할 수 없다")
    void negotiationInProgress() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of(DAY1));
        long applicationId = applicationScenario.submit(user, spaceId, DAY1, DAY1);
        mockMvc.perform(post("/api/v1/applications/{id}/offers", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 40000}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/provider/applications/{id}/approve", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PRICE_NEGOTIATION_IN_PROGRESS"));
    }

    @Test
    @DisplayName("확정 예약을 시작 전에 취소하면 그 날짜가 다시 열린다")
    void cancelApprovedReopensDates() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of(DAY1, DAY2));
        long applicationId = applicationScenario.submit(user, spaceId, DAY1, DAY2);
        mockMvc.perform(post("/api/v1/provider/applications/{id}/approve", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(jsonPath("$.data.availableDates.length()").value(0));

        mockMvc.perform(post("/api/v1/applications/{id}/cancel", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(jsonPath("$.data.availableDates.length()").value(2));
    }

    private void expectStatus(TestAccounts.Account applicant, long applicationId, String expected) throws Exception {
        mockMvc.perform(get("/api/v1/applications/{id}", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, applicant.bearer()))
                .andExpect(jsonPath("$.data.status").value(expected));
    }
}
