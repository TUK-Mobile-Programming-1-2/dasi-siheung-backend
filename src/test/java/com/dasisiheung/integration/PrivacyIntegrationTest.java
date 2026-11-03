package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.BookingScenario;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.TestAccounts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class PrivacyIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ClockConfig.SERVICE_ZONE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private BookingScenario bookingScenario;

    @Test
    @DisplayName("400일 전에 끝난 확정 예약: 파기하면 통계에 1번만 들어가고 신청서 글은 비식별화된다")
    void purgeOldBooking() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        long applicationId = bookingScenario.approvedBooking(provider, admin, user, TODAY.plusDays(1), TODAY.plusDays(2));
        bookingScenario.movePeriod(applicationId, -401, -400);
        YearMonth month = YearMonth.from(TODAY.minusDays(401));

        long before = monthlyBookings(admin, month);
        mockMvc.perform(post("/api/v1/admin/privacy/purge").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executedBy").value(admin.id()));
        long afterFirst = monthlyBookings(admin, month);
        mockMvc.perform(post("/api/v1/admin/privacy/purge").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk());
        long afterSecond = monthlyBookings(admin, month);

        assertThat(afterFirst).isEqualTo(before + 1);
        assertThat(afterSecond).isEqualTo(afterFirst);
        mockMvc.perform(get("/api/v1/applications/{id}", applicationId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.activityTitle").value("(파기됨)"))
                .andExpect(jsonPath("$.data.totalPrice").value(100_000));
        mockMvc.perform(get("/api/v1/admin/privacy/purge-logs").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(jsonPath("$.data.content[0].executedBy").value(admin.id()));
    }

    @Test
    @DisplayName("최근에 끝난 예약은 통계에만 들어가고 비식별화되지 않는다")
    void recentBookingKept() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        long applicationId = bookingScenario.approvedBooking(provider, admin, user, TODAY.plusDays(1), TODAY.plusDays(1));
        bookingScenario.movePeriod(applicationId, -3, -3);

        mockMvc.perform(post("/api/v1/admin/privacy/purge").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.activityTitle").value("수제 캔들 팝업 스토어"));
    }

    private long monthlyBookings(TestAccounts.Account admin, YearMonth month) throws Exception {
        String body = mockMvc.perform(get("/api/v1/admin/statistics/monthly")
                        .param("from", month.toString()).param("to", month.toString())
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(body).get("data");
        return data.get("totalBookings").asLong();
    }
}
