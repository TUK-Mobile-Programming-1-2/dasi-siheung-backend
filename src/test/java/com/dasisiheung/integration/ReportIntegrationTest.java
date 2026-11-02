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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Test
    @DisplayName("공간 신고 2건 → 중복 409 · 자기 신고 400 → 숨김 + 정지 → 둘 다 닫힘, 공간 404, 제공자 로그인 403 → 해제")
    void spaceReportFlow() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        TestAccounts.Account reporterA = testAccounts.create(Role.USER);
        TestAccounts.Account reporterB = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, admin, Set.of());

        long reportId = report(reporterA, "SPACE", spaceId).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);
        long otherReportId = report(reporterB, "SPACE", spaceId).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);
        report(reporterA, "SPACE", spaceId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_REPORT"));
        report(provider, "SPACE", spaceId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CANNOT_REPORT_SELF"));

        mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(jsonPath("$.data.pendingReportCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        mockMvc.perform(post("/api/v1/admin/reports/{id}/resolve", reportId).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"HIDE_AND_SUSPEND\", \"suspendDays\": 7, \"note\": \"허위 사진 확인\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.targetVisible").value(false))
                .andExpect(jsonPath("$.data.ownerStatus").value("SUSPENDED"));

        mockMvc.perform(get("/api/v1/admin/reports/{id}", otherReportId).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/provider/spaces/{id}", spaceId).header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.status").value("HIDDEN"))
                .andExpect(jsonPath("$.data.reviewComment").value("허위 사진 확인"));
        login(provider).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("ACCOUNT_SUSPENDED"));

        mockMvc.perform(post("/api/v1/admin/members/{id}/unsuspend", provider.id()).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        login(provider).andExpect(status().isOk());
    }

    @Test
    @DisplayName("댓글 신고를 숨김 처리하면 댓글 목록에서 빠지고 댓글 수가 줄어든다")
    void commentReport() throws Exception {
        TestAccounts.Account writer = testAccounts.create(Role.USER);
        TestAccounts.Account commenter = testAccounts.create(Role.USER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        long postId = mockMvc.perform(post("/api/v1/community/posts").header(HttpHeaders.AUTHORIZATION, writer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"QUESTION\", \"title\": \"질문\", \"content\": \"내용\"}"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);
        long commentId = mockMvc.perform(post("/api/v1/community/posts/{id}/comments", postId)
                        .header(HttpHeaders.AUTHORIZATION, commenter.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"광고 댓글\"}"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);

        long reportId = report(writer, "COMMENT", commentId).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);
        mockMvc.perform(post("/api/v1/admin/reports/{id}/resolve", reportId).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"HIDE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/community/posts/{id}/comments", postId))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(get("/api/v1/community/posts/{id}", postId))
                .andExpect(jsonPath("$.data.commentCount").value(0));
    }

    private ResultActions report(TestAccounts.Account reporter, String targetType, long targetId) throws Exception {
        return mockMvc.perform(post("/api/v1/reports").header(HttpHeaders.AUTHORIZATION, reporter.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\": \"%s\", \"targetId\": %d, \"reason\": \"FALSE_INFORMATION\"}"
                        .formatted(targetType, targetId)));
    }

    private ResultActions login(TestAccounts.Account account) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(account.email(), TestAccounts.PASSWORD)));
    }

    private long idOf(String body) {
        try {
            return objectMapper.readTree(body).at("/data/id").asLong();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
