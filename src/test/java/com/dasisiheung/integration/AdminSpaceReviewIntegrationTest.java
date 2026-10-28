package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
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
class AdminSpaceReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Test
    @DisplayName("증빙 없는 공간은 승인할 수 없고, 수정 요청 → 다시 검수 요청 → 승인하면 공개되며 이력이 남는다")
    void reviewFlow() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        long spaceId = spaceScenario.register(provider, Set.of());

        mockMvc.perform(post("/api/v1/admin/spaces/{id}/reviews", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(review("APPROVE", null, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SPACE_PROOF_REQUIRED"));

        spaceScenario.addProof(provider, spaceId);
        mockMvc.perform(post("/api/v1/admin/spaces/{id}/reviews", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(review("REQUEST_REVISION", "호실이 증빙과 다릅니다.", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.space.status").value("REVISION_REQUESTED"));

        mockMvc.perform(get("/api/v1/provider/spaces/{id}", spaceId).header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.reviewComment").value("호실이 증빙과 다릅니다."));

        mockMvc.perform(post("/api/v1/provider/spaces/{id}/resubmit", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        mockMvc.perform(post("/api/v1/admin/spaces/{id}/reviews", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(review("APPROVE", null, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.space.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.reviews.length()").value(2))
                .andExpect(jsonPath("$.data.reviews[0].decision").value("APPROVE"));
    }

    @Test
    @DisplayName("검수 대기 목록에는 검수 대기 공간만 나온다")
    void pendingQueue() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        spaceScenario.register(provider, Set.of());
        spaceScenario.publishedSpace(provider, admin, Set.of());

        mockMvc.perform(get("/api/v1/admin/spaces").param("status", "PENDING_REVIEW").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.content[?(@.status != 'PENDING_REVIEW')]").isEmpty());
    }

    private static String review(String decision, String reason, boolean allPassed) {
        return """
                {"decision": "%s", "reason": %s, "checklist": {"authorityVerified": true, "addressMatched": true,
                 "buildingUnitMatched": %s, "areaMatched": true, "usageMatched": true}}
                """.formatted(decision, reason == null ? "null" : "\"" + reason + "\"", allPassed);
    }
}
