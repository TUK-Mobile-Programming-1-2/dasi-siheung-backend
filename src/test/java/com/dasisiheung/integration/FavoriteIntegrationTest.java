package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class FavoriteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Test
    @DisplayName("두 번 찜해도 1, 상세에 favorited, 내 목록에 보이고, 두 번 해제해도 0")
    void favoriteFlow() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of());

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(put("/api/v1/spaces/{id}/favorite", spaceId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favoriteCount").value(1));
        }

        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.favorited").value(true))
                .andExpect(jsonPath("$.data.favoriteCount").value(1));
        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(jsonPath("$.data.favorited").value(false));

        mockMvc.perform(get("/api/v1/members/me/favorites").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(spaceId));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(delete("/api/v1/spaces/{id}/favorite", spaceId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favorited").value(false))
                    .andExpect(jsonPath("$.data.favoriteCount").value(0));
        }
    }

    @Test
    @DisplayName("공간을 수정해도 찜 수는 덮어써지지 않고, 일시 중단한 공간은 내 찜 목록에서 빠진다")
    void favoriteCountSurvivesSpaceUpdate() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of());
        mockMvc.perform(put("/api/v1/spaces/{id}/favorite", spaceId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/provider/spaces/{id}/pause", spaceId).header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/members/me/favorites").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(post("/api/v1/provider/spaces/{id}/resume", spaceId).header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(jsonPath("$.data.favoriteCount").value(1));
    }
}
