package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.repository.PushTokenRepository;
import com.dasisiheung.service.ChatMessageSentEvent;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
@RecordApplicationEvents
class PushTokenIntegrationTest {

    private static final String PATH = "/api/v1/members/me/push-tokens";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @Autowired
    private ApplicationEvents events;

    @Test
    @DisplayName("등록은 여러 번 해도 하나, 다른 계정으로 등록하면 옮겨 가고, 해제는 내 토큰만 지운다")
    void registerMoveAndUnregister() throws Exception {
        TestAccounts.Account user = testAccounts.create(Role.USER);
        TestAccounts.Account other = testAccounts.create(Role.PROVIDER);
        String token = newToken();

        register(user, token).andExpect(status().isOk());
        register(user, token).andExpect(status().isOk());
        assertThat(pushTokenRepository.findTokensByMemberId(user.id())).containsExactly(token);

        register(other, token).andExpect(status().isOk());
        assertThat(pushTokenRepository.findTokensByMemberId(user.id())).isEmpty();
        assertThat(pushTokenRepository.findTokensByMemberId(other.id())).containsExactly(token);

        unregister(user, token).andExpect(status().isOk());
        assertThat(pushTokenRepository.findTokensByMemberId(other.id())).containsExactly(token);
        unregister(other, token).andExpect(status().isOk());
        assertThat(pushTokenRepository.findByToken(token)).isEmpty();
        unregister(other, token).andExpect(status().isOk());
    }

    @Test
    @DisplayName("탈퇴하면 그 회원의 모든 기기 토큰을 지운다")
    void withdrawDeletesTokens() throws Exception {
        TestAccounts.Account user = testAccounts.create(Role.USER);
        register(user, newToken()).andExpect(status().isOk());
        register(user, newToken()).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/members/me/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"%s\"}".formatted(TestAccounts.PASSWORD)))
                .andExpect(status().isOk());

        assertThat(pushTokenRepository.findTokensByMemberId(user.id())).isEmpty();
    }

    @Test
    @DisplayName("메시지를 보내면 대화 상대에게 보낼 푸시 이벤트가 생긴다 (발송은 커밋 뒤)")
    void sendMessagePublishesEvent() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of());
        long conversationId = readId(mockMvc.perform(post("/api/v1/spaces/{id}/conversations", spaceId)
                .header(HttpHeaders.AUTHORIZATION, user.bearer())).andExpect(status().isOk()));

        long messageId = readId(mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"이번 주말 가능할까요?\"}"))
                .andExpect(status().isCreated()));

        assertThat(events.stream(ChatMessageSentEvent.class)).singleElement().satisfies(event -> {
            assertThat(event.conversationId()).isEqualTo(conversationId);
            assertThat(event.messageId()).isEqualTo(messageId);
            assertThat(event.senderId()).isEqualTo(user.id());
            assertThat(event.recipientId()).isEqualTo(provider.id());
            assertThat(event.content()).isEqualTo("이번 주말 가능할까요?");
        });
    }

    private ResultActions register(TestAccounts.Account account, String token) throws Exception {
        return mockMvc.perform(put(PATH)
                .header(HttpHeaders.AUTHORIZATION, account.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"%s\"}".formatted(token)));
    }

    private ResultActions unregister(TestAccounts.Account account, String token) throws Exception {
        return mockMvc.perform(delete(PATH).param("token", token).header(HttpHeaders.AUTHORIZATION, account.bearer()));
    }

    private long readId(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private static String newToken() {
        return "it-" + UUID.randomUUID();
    }
}
