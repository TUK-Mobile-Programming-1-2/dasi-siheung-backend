package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Facility;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceFixtures;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
class RecommendationIntegrationTest {

    /** 다른 테스트 데이터와 겹치지 않을 먼 날짜. */
    private static final LocalDate FAR_DAY = LocalDate.now(ClockConfig.SERVICE_ZONE).plusDays(87);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Test
    @DisplayName("목적에 맞는 테마의 공간이 위로, 관리자가 테마를 보정하면 순위가 바뀐다")
    void purposeRanking() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        long cafe = publish(provider, admin, "추천 카페거리", CommercialTheme.CAFE_STREET);
        long industrial = publish(provider, admin, "추천 공단", CommercialTheme.INDUSTRIAL);

        List<Long> ranking = recommend("PROMOTION");
        assertThat(ranking.indexOf(cafe)).isLessThan(ranking.indexOf(industrial));

        mockMvc.perform(put("/api/v1/admin/spaces/{id}/theme", industrial).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"commercialTheme\": \"WATERFRONT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.space.commercialTheme").value("WATERFRONT"))
                .andExpect(jsonPath("$.data.space.status").value("PUBLISHED"));
        List<Long> afterCorrection = recommend("PROMOTION");
        assertThat(afterCorrection).contains(cafe, industrial);

        mockMvc.perform(get("/api/v1/spaces/recommendations").param("startDate", FAR_DAY.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    private long publish(TestAccounts.Account provider, TestAccounts.Account admin, String title,
                         CommercialTheme theme) throws Exception {
        return spaceScenario.publishedSpace(provider, admin, SpaceFixtures.request(title, SpaceFixtures.JEONGWANG_LAT,
                SpaceFixtures.JEONGWANG_LNG, theme, 30_000, Set.of(Facility.WIFI), Set.of(ActivityType.CLASS),
                Set.of(FAR_DAY)));
    }

    private List<Long> recommend(String purpose) throws Exception {
        String body = mockMvc.perform(get("/api/v1/spaces/recommendations").param("purpose", purpose)
                        .param("activityType", "CLASS").param("startDate", FAR_DAY.toString())
                        .param("endDate", FAR_DAY.toString()).param("limit", "20"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Long> ids = new ArrayList<>();
        for (JsonNode node : objectMapper.readTree(body).get("data")) {
            ids.add(node.at("/space/id").asLong());
        }
        return ids;
    }
}
