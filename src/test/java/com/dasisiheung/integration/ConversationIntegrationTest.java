package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
