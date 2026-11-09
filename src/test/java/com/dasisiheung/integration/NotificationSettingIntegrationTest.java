package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.repository.NotificationSettingRepository;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.TestAccounts;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
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
class NotificationSettingIntegrationTest {

    private static final String PATH = "/api/v1/members/me/notification-settings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("저장한 적이 없으면 기본값을 주고 행을 만들지 않는다")
    void defaultsWithoutRow() throws Exception {
        TestAccounts.Account account = testAccounts.create(Role.USER);

        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, account.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.push").value(true))
                .andExpect(jsonPath("$.data.chat").value(true))
                .andExpect(jsonPath("$.data.activity").value(true))
                .andExpect(jsonPath("$.data.favoriteSpace").value(false))
                .andExpect(jsonPath("$.data.marketing").value(false))
                .andExpect(jsonPath("$.data.dndEnabled").value(true))
                .andExpect(jsonPath("$.data.dndStartHour").value(22))
                .andExpect(jsonPath("$.data.dndEndHour").value(8))
                .andExpect(jsonPath("$.data.marketingConsentedAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());

        assertThat(notificationSettingRepository.existsById(account.id())).isFalse();
    }

    @Test
    @DisplayName("저장하면 DB 에서 다시 읽어도 같은 값이고, 광고 동의 시각은 철회할 때까지 유지된다")
    void replaceAndGet() throws Exception {
        TestAccounts.Account account = testAccounts.create(Role.PROVIDER);

        String firstConsent = consentedAt(replace(account, true, 23, 7)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chat").value(false))
                .andExpect(jsonPath("$.data.dndStartHour").value(23))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty()));
        assertThat(firstConsent).isNotNull();
        clearPersistenceContext();

        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, account.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.push").value(true))
                .andExpect(jsonPath("$.data.chat").value(false))
                .andExpect(jsonPath("$.data.favoriteSpace").value(true))
                .andExpect(jsonPath("$.data.marketing").value(true))
                .andExpect(jsonPath("$.data.dndStartHour").value(23))
                .andExpect(jsonPath("$.data.dndEndHour").value(7))
                .andExpect(jsonPath("$.data.marketingConsentedAt").value(firstConsent));
        clearPersistenceContext();

        replace(account, true, 1, 6)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dndStartHour").value(1))
                .andExpect(jsonPath("$.data.marketingConsentedAt").value(firstConsent));
        clearPersistenceContext();

        replace(account, false, 1, 6)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketing").value(false))
                .andExpect(jsonPath("$.data.marketingConsentedAt").doesNotExist());
        assertThat(notificationSettingRepository.count()).isPositive();
    }

    @Test
    @DisplayName("시각이 범위를 벗어나면 400 INVALID_INPUT 이고 아무것도 저장하지 않는다")
    void rejectOutOfRange() throws Exception {
        TestAccounts.Account account = testAccounts.create(Role.USER);

        replace(account, false, 24, 8)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("dndStartHour"));

        assertThat(notificationSettingRepository.existsById(account.id())).isFalse();
    }

    @Test
    @DisplayName("탈퇴하면 알림 설정도 지운다")
    void withdrawDeletesSettings() throws Exception {
        TestAccounts.Account account = testAccounts.create(Role.USER);
        replace(account, true, 22, 8).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/members/me/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "%s"}
                                """.formatted(TestAccounts.PASSWORD)))
                .andExpect(status().isOk());

        assertThat(notificationSettingRepository.existsById(account.id())).isFalse();
        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, account.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"));
    }

    /** push · activity · favoriteSpace 켜짐, chat 꺼짐, 방해금지 켜짐. */
    private ResultActions replace(TestAccounts.Account account, boolean marketing, int startHour, int endHour)
            throws Exception {
        return mockMvc.perform(put(PATH)
                .header(HttpHeaders.AUTHORIZATION, account.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"push": true, "chat": false, "activity": true, "favoriteSpace": true, "marketing": %s,
                         "dndEnabled": true, "dndStartHour": %d, "dndEndHour": %d}
                        """.formatted(marketing, startHour, endHour)));
    }

    private static String consentedAt(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.marketingConsentedAt");
    }

    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
