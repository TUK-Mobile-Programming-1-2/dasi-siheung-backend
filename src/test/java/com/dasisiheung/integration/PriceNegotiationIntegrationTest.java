package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
class PriceNegotiationIntegrationTest {

    private static final LocalDate DAY1 = LocalDate.now(ClockConfig.SERVICE_ZONE).plusDays(1);
    private static final LocalDate DAY2 = DAY1.plusDays(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Test
    @DisplayName("신청과 함께 제안 → 재제안 409 → 제공자 역제안 → 제공자 수락 409 → 신청자 수락으로 합의")
    void negotiationFlow() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of(DAY1, DAY2));

        String body = mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ApplicationFixtures.requestWithOffer(spaceId, DAY1, DAY2, 80_000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.negotiationStatus").value("WAITING_PROVIDER"))
                .andExpect(jsonPath("$.data.offers.length()").value(1))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long applicationId = objectMapper.readTree(body).at("/data/id").asLong();

        offer(user, applicationId, 70_000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("OFFER_NOT_ALLOWED"));
        offer(provider, applicationId, 100_001)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_OFFER_PRICE"));
        offer(testAccounts.create(Role.USER), applicationId, 90_000)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_APPLICATION_PARTICIPANT"));

        offer(provider, applicationId, 95_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.negotiationStatus").value("WAITING_APPLICANT"))
                .andExpect(jsonPath("$.data.pendingOfferAmount").value(95_000));

        accept(provider, applicationId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NO_PENDING_OFFER"));

        accept(user, applicationId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.negotiationStatus").value("AGREED"))
                .andExpect(jsonPath("$.data.totalPrice").value(95_000))
                .andExpect(jsonPath("$.data.pendingOfferAmount").doesNotExist())
                .andExpect(jsonPath("$.data.offers.length()").value(2))
                .andExpect(jsonPath("$.data.offers[1].proposerSide").value("PROVIDER"))
                .andExpect(jsonPath("$.data.offers[1].acceptedAt").exists());
    }

    @Test
    @DisplayName("제공자 역제안 → 신청자 알림함 · 읽음 → 거절(검토 중 유지) → 제공자 알림 → 재제안 → 수락")
    void counterOfferNotificationAndRejection() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of(DAY1, DAY2));
        long applicationId = submit(user, spaceId);

        offer(provider, applicationId, 95_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.negotiationStatus").value("WAITING_APPLICANT"))
                .andExpect(jsonPath("$.data.respondingSide").value("APPLICANT"))
                .andExpect(jsonPath("$.data.offerableSides[0]").value("APPLICANT"))
                .andExpect(jsonPath("$.data.offerableSides.length()").value(1));

        String inbox = notifications(user, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("PRICE_OFFER_RECEIVED"))
                .andExpect(jsonPath("$.data.content[0].applicationId").value(applicationId))
                .andExpect(jsonPath("$.data.content[0].title").value("공간 제공자의 역제안"))
                .andExpect(jsonPath("$.data.content[0].read").value(false))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long notificationId = objectMapper.readTree(inbox).at("/data/content/0/id").asLong();
        notifications(provider, false).andExpect(jsonPath("$.data.totalElements").value(0));
        unreadCount(user).andExpect(jsonPath("$.data.count").value(1));

        mockMvc.perform(post("/api/v1/notifications/{id}/read", notificationId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOTIFICATION_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/notifications/{id}/read", notificationId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));
        unreadCount(user).andExpect(jsonPath("$.data.count").value(0));

        reject(provider, applicationId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NO_PENDING_OFFER"));
        reject(user, applicationId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.negotiationStatus").value("OFFER_REJECTED"))
                .andExpect(jsonPath("$.data.totalPrice").value(100_000))
                .andExpect(jsonPath("$.data.pendingOfferAmount").doesNotExist())
                .andExpect(jsonPath("$.data.respondingSide").doesNotExist())
                .andExpect(jsonPath("$.data.offerableSides.length()").value(2))
                .andExpect(jsonPath("$.data.offers[0].rejectedAt").exists())
                .andExpect(jsonPath("$.data.offers[0].rejectionMessage").value("주말이라 어렵습니다."));
        reject(user, applicationId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NO_PENDING_OFFER"));

        notifications(provider, true)
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("PRICE_OFFER_REJECTED"))
                .andExpect(jsonPath("$.data.content[0].applicationId").value(applicationId));

        offer(provider, applicationId, 90_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.negotiationStatus").value("WAITING_APPLICANT"));
        accept(user, applicationId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.negotiationStatus").value("AGREED"))
                .andExpect(jsonPath("$.data.totalPrice").value(90_000))
                .andExpect(jsonPath("$.data.offers.length()").value(2))
                .andExpect(jsonPath("$.data.offers[1].acceptedAt").exists());

        notifications(provider, false)
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].type").value("PRICE_OFFER_ACCEPTED"))
                .andExpect(jsonPath("$.data.content[1].type").value("PRICE_OFFER_REJECTED"));
        mockMvc.perform(post("/api/v1/notifications/read-all").header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2));
        unreadCount(provider).andExpect(jsonPath("$.data.count").value(0));
        unreadCount(user).andExpect(jsonPath("$.data.count").value(1));
    }

    private long submit(TestAccounts.Account user, long spaceId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationFixtures.request(spaceId, DAY1, DAY2))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private ResultActions reject(TestAccounts.Account member, long applicationId) throws Exception {
        return mockMvc.perform(post("/api/v1/applications/{id}/offers/reject", applicationId)
                .header(HttpHeaders.AUTHORIZATION, member.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"주말이라 어렵습니다.\"}"));
    }

    private ResultActions notifications(TestAccounts.Account member, boolean unreadOnly) throws Exception {
        return mockMvc.perform(get("/api/v1/notifications").param("unreadOnly", String.valueOf(unreadOnly))
                .header(HttpHeaders.AUTHORIZATION, member.bearer()));
    }

    private ResultActions unreadCount(TestAccounts.Account member) throws Exception {
        return mockMvc.perform(get("/api/v1/notifications/unread-count")
                .header(HttpHeaders.AUTHORIZATION, member.bearer()));
    }

    private ResultActions offer(TestAccounts.Account member, long applicationId, int amount) throws Exception {
        return mockMvc.perform(post("/api/v1/applications/{id}/offers", applicationId)
                .header(HttpHeaders.AUTHORIZATION, member.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": %d, \"message\": \"제안\"}".formatted(amount)));
    }

    private ResultActions accept(TestAccounts.Account member, long applicationId) throws Exception {
        return mockMvc.perform(post("/api/v1/applications/{id}/offers/accept", applicationId)
                .header(HttpHeaders.AUTHORIZATION, member.bearer()));
    }
}
