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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ApplicationIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ClockConfig.SERVICE_ZONE);
    private static final LocalDate DAY1 = TODAY.plusDays(1);
    private static final LocalDate DAY2 = TODAY.plusDays(2);

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

    private TestAccounts.Account provider;
    private TestAccounts.Account user;
    private long spaceId;

    @BeforeEach
    void setUp() throws Exception {
        provider = testAccounts.create(Role.PROVIDER);
        user = testAccounts.create(Role.USER);
        spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of(DAY1, DAY2));
    }

    @Test
    @DisplayName("신청 → 중복 신청 409 → 내 목록 · 제공자 상세 → 다른 회원 403 → 취소")
    void submitFlow() throws Exception {
        long applicationId = applicationScenario.submit(user, spaceId, DAY1, DAY2);

        mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationFixtures.request(spaceId, DAY2, DAY2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_APPLICATION"));

        mockMvc.perform(get("/api/v1/applications").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(applicationId))
                .andExpect(jsonPath("$.data.content[0].totalPrice").value(100_000));

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicant.id").value(user.id()))
                .andExpect(jsonPath("$.data.days").value(2));

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, testAccounts.create(Role.USER).bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_APPLICATION_PARTICIPANT"));

        mockMvc.perform(post("/api/v1/applications/{id}/cancel", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"일정 변경\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.statusReason").value("일정 변경"));

        // 취소한 뒤에는 같은 기간으로 다시 신청할 수 있다
        applicationScenario.submit(user, spaceId, DAY2, DAY2);
    }

    @Test
    @DisplayName("사용 가능 날짜 밖은 409 PERIOD_NOT_AVAILABLE, 공간 제공자는 403")
    void rejectedSubmissions() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ApplicationFixtures.request(spaceId, DAY1, TODAY.plusDays(3)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_AVAILABLE"));

        mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationFixtures.request(spaceId, DAY1, DAY1))))
                .andExpect(status().isForbidden());
    }
}
