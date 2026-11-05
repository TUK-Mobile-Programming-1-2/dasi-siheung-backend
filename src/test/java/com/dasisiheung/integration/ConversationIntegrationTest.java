package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.ApplicationScenario;
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
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ConversationIntegrationTest {

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
    @DisplayName("대화 열기(멱등) → 이용자 2건 → 제공자 안 읽음 2 → 폴링 → 읽음 → 답장 → 이용자 안 읽음 1")
    void conversationFlow() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of());

        long conversationId = open(user, spaceId);
        mockMvc.perform(post("/api/v1/spaces/{id}/conversations", spaceId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.id").value(conversationId));

        long first = send(user, conversationId, "안녕하세요");
        long second = send(user, conversationId, "11월 첫 주말 가능할까요?");

        mockMvc.perform(get("/api/v1/conversations").header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.content[0].id").value(conversationId))
                .andExpect(jsonPath("$.data.content[0].unreadCount").value(2))
                .andExpect(jsonPath("$.data.content[0].counterpart.role").value("USER"))
                .andExpect(jsonPath("$.data.content[0].lastMessagePreview").value("11월 첫 주말 가능할까요?"));
        mockMvc.perform(get("/api/v1/conversations/unread-count").header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.count").value(2));
        mockMvc.perform(get("/api/v1/conversations/unread-count").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.count").value(0));

        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId).param("after", String.valueOf(first))
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(second))
                .andExpect(jsonPath("$.data[0].mine").value(false));
        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data[0].id").value(first))
                .andExpect(jsonPath("$.data[1].id").value(second));

        mockMvc.perform(post("/api/v1/conversations/{id}/read", conversationId).header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lastMessageId\": " + second + "}"))
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        send(provider, conversationId, "네 가능합니다. 신청서 보내 주세요.");
        mockMvc.perform(get("/api/v1/conversations/unread-count").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.count").value(1));

        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, testAccounts.create(Role.USER).bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_CONVERSATION_MEMBER"));
    }

    @Test
    @DisplayName("받은 신청에서 제공자가 대화를 연다 → 신청 상세 · 목록에 conversationId → 이용자가 공간에서 열어도 같은 대화 → 남은 403")
    void openFromApplication() throws Exception {
        LocalDate day = LocalDate.now(ClockConfig.SERVICE_ZONE).plusDays(1);
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of(day));
        long applicationId = applicationScenario.submit(user, spaceId, day, day);

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.conversationId").doesNotExist());

        String body = mockMvc.perform(post("/api/v1/applications/{id}/conversation", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spaceId").value(spaceId))
                .andExpect(jsonPath("$.data.counterpart.id").value(user.id()))
                .andExpect(jsonPath("$.data.counterpart.role").value("USER"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long conversationId = objectMapper.readTree(body).at("/data/id").asLong();

        mockMvc.perform(post("/api/v1/applications/{id}/conversation", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.id").value(conversationId))
                .andExpect(jsonPath("$.data.counterpart.role").value("PROVIDER"));
        mockMvc.perform(post("/api/v1/spaces/{id}/conversations", spaceId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.id").value(conversationId));

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId).header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.conversationId").value(conversationId));
        mockMvc.perform(get("/api/v1/provider/applications").header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.content[0].id").value(applicationId))
                .andExpect(jsonPath("$.data.content[0].conversationId").value(conversationId));
        mockMvc.perform(get("/api/v1/applications").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.content[0].conversationId").value(conversationId));

        send(provider, conversationId, "신청서 확인했습니다. 반입 물품 알려 주세요.");
        mockMvc.perform(post("/api/v1/applications/{id}/conversation", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        mockMvc.perform(post("/api/v1/applications/{id}/conversation", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, testAccounts.create(Role.PROVIDER).bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_APPLICATION_PARTICIPANT"));
    }

    private long open(TestAccounts.Account user, long spaceId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/spaces/{id}/conversations", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private long send(TestAccounts.Account sender, long conversationId, String content) throws Exception {
        String body = mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, sender.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + content + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/id").asLong();
    }
}
