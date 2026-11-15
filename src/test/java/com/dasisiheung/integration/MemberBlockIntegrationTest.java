package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.support.ApplicationScenario;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class MemberBlockIntegrationTest {

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

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("차단(멱등) → 목록 · blockedByMe → 양쪽 전송 403 · 기존 대화는 그대로 → 새 대화 403 (공간 · 신청) → 해제 → 다시 전송")
    void blockFlow() throws Exception {
        LocalDate day = LocalDate.now(ClockConfig.SERVICE_ZONE).plusDays(1);
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        long spaceId = spaceScenario.publishedSpace(provider, admin, Set.of(day));
        long conversationId = json(mockMvc.perform(post("/api/v1/spaces/{id}/conversations", spaceId)
                .header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.blockedByMe").value(false))).at("/data/id").asLong();
        send(provider, conversationId, "안녕하세요. 문의 주셔서 감사합니다.").andExpect(status().isCreated());

        String blockedAt = json(block(user, provider.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(provider.id()))
                .andExpect(jsonPath("$.data.name").value("통합테스트"))).at("/data/blockedAt").asText();
        assertThat(blockedAt).as("DB 에 저장되는 정밀도(마이크로초)까지만 내려간다")
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,6})?");
        block(user, provider.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blockedAt").value(blockedAt));
        mockMvc.perform(get("/api/v1/members/me/blocks").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(provider.id()));

        mockMvc.perform(get("/api/v1/conversations").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.content[0].id").value(conversationId))
                .andExpect(jsonPath("$.data.content[0].blockedByMe").value(true));
        mockMvc.perform(get("/api/v1/conversations").header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.content[0].blockedByMe").value(false));
        mockMvc.perform(get("/api/v1/conversations/{id}", conversationId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.blockedByMe").value(true));

        send(user, conversationId, "차단한 사람이 보냄")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("BLOCKED_MEMBER"));
        send(provider, conversationId, "차단당한 사람이 보냄")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("BLOCKED_MEMBER"));
        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(post("/api/v1/spaces/{id}/conversations", spaceId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(conversationId))
                .andExpect(jsonPath("$.data.blockedByMe").value(true));

        long otherSpaceId = spaceScenario.publishedSpace(provider, admin, Set.of(day));
        mockMvc.perform(post("/api/v1/spaces/{id}/conversations", otherSpaceId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("BLOCKED_MEMBER"));
        long applicationId = applicationScenario.submit(user, otherSpaceId, day, day);
        mockMvc.perform(post("/api/v1/applications/{id}/conversation", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("BLOCKED_MEMBER"));

        unblock(user, provider.id()).andExpect(status().isOk());
        unblock(user, provider.id()).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/members/me/blocks").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        send(user, conversationId, "차단을 풀고 다시 보냄").andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/conversations/{id}", conversationId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.blockedByMe").value(false));
    }

    @Test
    @DisplayName("본인 400 · 없는 회원 404 · 탈퇴 회원은 새로 차단 못 하지만 이미 차단했으면 목록에 남아 해제할 수 있다 · 최근 차단 순")
    void edgeCases() throws Exception {
        TestAccounts.Account me = testAccounts.create(Role.USER);
        TestAccounts.Account first = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account second = testAccounts.create(Role.USER);
        TestAccounts.Account withdrawn = testAccounts.create(Role.USER);

        block(me, me.id())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CANNOT_BLOCK_SELF"));
        block(me, Long.MAX_VALUE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"));
        withdraw(withdrawn);
        block(me, withdrawn.id())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"));

        block(me, first.id()).andExpect(status().isOk());
        block(me, second.id()).andExpect(status().isOk());
        withdraw(first);
        mockMvc.perform(get("/api/v1/members/me/blocks").header(HttpHeaders.AUTHORIZATION, me.bearer()))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(second.id()))
                .andExpect(jsonPath("$.data.content[1].id").value(first.id()))
                .andExpect(jsonPath("$.data.content[1].name").value("탈퇴한 회원"))
                .andExpect(jsonPath("$.data.content[1].profileImageUrl").doesNotExist());
        mockMvc.perform(get("/api/v1/members/me/blocks").header(HttpHeaders.AUTHORIZATION, second.bearer()))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        unblock(me, first.id()).andExpect(status().isOk());
        String body = mockMvc.perform(get("/api/v1/members/me/blocks").header(HttpHeaders.AUTHORIZATION, me.bearer()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(body).at("/data/content").findValuesAsText("id"))
                .containsExactly(String.valueOf(second.id()));
    }

    private ResultActions block(TestAccounts.Account member, long targetId) throws Exception {
        return mockMvc.perform(put("/api/v1/members/me/blocks/{id}", targetId).header(HttpHeaders.AUTHORIZATION, member.bearer()));
    }

    private ResultActions unblock(TestAccounts.Account member, long targetId) throws Exception {
        return mockMvc.perform(delete("/api/v1/members/me/blocks/{id}", targetId)
                .header(HttpHeaders.AUTHORIZATION, member.bearer()));
    }

    private ResultActions send(TestAccounts.Account sender, long conversationId, String content) throws Exception {
        return mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                .header(HttpHeaders.AUTHORIZATION, sender.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"" + content + "\"}"));
    }

    private void withdraw(TestAccounts.Account account) {
        memberRepository.findById(account.id()).orElseThrow().withdraw(LocalDateTime.now(clock));
        memberRepository.flush();
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
