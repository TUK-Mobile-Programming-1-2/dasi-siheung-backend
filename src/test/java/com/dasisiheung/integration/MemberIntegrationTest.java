package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.ApplicationScenario;
import com.dasisiheung.support.BookingScenario;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class MemberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Autowired
    private ApplicationScenario applicationScenario;

    @Autowired
    private BookingScenario bookingScenario;

    @Test
    @DisplayName("내 정보를 수정하고 다시 조회하면 바뀐 값이 보인다")
    void updateAndGet() throws Exception {
        TestAccounts.Account account = testAccounts.create(Role.USER);

        mockMvc.perform(patch("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "010-5555-6666"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, account.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("01055556666"))
                .andExpect(jsonPath("$.data.email").value(account.email()));
    }

    @Test
    @DisplayName("탈퇴하면 같은 이메일·비밀번호로 로그인할 수 없다")
    void withdrawThenCannotLogin() throws Exception {
        TestAccounts.Account account = testAccounts.create(Role.PROVIDER);

        mockMvc.perform(post("/api/v1/members/me/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "%s"}
                                """.formatted(TestAccounts.PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(account.email(), TestAccounts.PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("검토 중인 신청이 있으면 이용자 · 공간 제공자 모두 탈퇴 409, 신청을 취소하면 탈퇴된다")
    void withdrawBlockedByPendingApplication() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        LocalDate day = today().plusDays(3);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of(day));
        long applicationId = applicationScenario.submit(user, spaceId, day, day);

        withdraw(user).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("WITHDRAWAL_BLOCKED_BY_APPLICATION"));
        withdraw(provider).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("WITHDRAWAL_BLOCKED_BY_APPLICATION"));

        mockMvc.perform(post("/api/v1/applications/{id}/cancel", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        withdraw(user).andExpect(status().isOk());
        withdraw(provider).andExpect(status().isOk());
    }

    @Test
    @DisplayName("확정 예약은 이용 마지막 날까지 탈퇴 409, 이용이 끝난 다음 날부터 탈퇴된다")
    void withdrawBlockedUntilBookingEnds() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        LocalDate day = today().plusDays(3);
        long applicationId = bookingScenario.approvedBooking(provider, testAccounts.create(Role.ADMIN), user, day, day);

        bookingScenario.movePeriod(applicationId, -1, 0);
        withdraw(user).andExpect(status().isConflict());
        withdraw(provider).andExpect(status().isConflict());

        bookingScenario.movePeriod(applicationId, -2, -1);
        withdraw(user).andExpect(status().isOk());
        withdraw(provider).andExpect(status().isOk());
    }

    private ResultActions withdraw(TestAccounts.Account account) throws Exception {
        return mockMvc.perform(post("/api/v1/members/me/withdraw")
                .header(HttpHeaders.AUTHORIZATION, account.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"password": "%s"}
                        """.formatted(TestAccounts.PASSWORD)));
    }

    private static LocalDate today() {
        return LocalDate.now(ClockConfig.SERVICE_ZONE);
    }
}
