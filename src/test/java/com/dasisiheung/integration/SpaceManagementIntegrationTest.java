package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceFixtures;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class SpaceManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceRepository spaceRepository;

    @Test
    @DisplayName("내 공간 목록 · 상세를 조회하고, 다른 제공자는 상세를 볼 수 없다")
    void listAndDetail() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        long spaceId = register(provider);

        mockMvc.perform(get("/api/v1/provider/spaces").header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING_REVIEW"));
        mockMvc.perform(get("/api/v1/provider/spaces").param("status", "PUBLISHED")
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        TestAccounts.Account other = testAccounts.create(Role.PROVIDER);
        mockMvc.perform(get("/api/v1/provider/spaces/{id}", spaceId).header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_SPACE_OWNER"));
    }

    @Test
    @DisplayName("공개 중 가격만 바꾸면 공개 유지, 용도를 바꾸면 다시 검수 대기")
    void reReviewOnReviewedFieldChange() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        long spaceId = register(provider);
        publish(spaceId);

        String priceChanged = objectMapper.writeValueAsString(SpaceFixtures.request(Set.of()))
                .replace("\"dailyPrice\":50000", "\"dailyPrice\":80000");
        mockMvc.perform(put("/api/v1/provider/spaces/{id}", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(priceChanged))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.dailyPrice").value(80000));

        String useChanged = priceChanged.replace("\"NEIGHBORHOOD_1\"", "\"SALES\"");
        mockMvc.perform(put("/api/v1/provider/spaces/{id}", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(useChanged))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
    }

    @Test
    @DisplayName("검수 대기 중에는 일시 중단할 수 없다 (409), 공개 후에는 중단 · 재개할 수 있다")
    void pauseAndResume() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        long spaceId = register(provider);

        mockMvc.perform(post("/api/v1/provider/spaces/{id}/pause", spaceId).header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_SPACE_STATUS"));

        publish(spaceId);
        mockMvc.perform(post("/api/v1/provider/spaces/{id}/pause", spaceId).header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.status").value("PAUSED"));
        mockMvc.perform(post("/api/v1/provider/spaces/{id}/resume", spaceId).header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("캘린더를 통째로 바꾼다")
    void changeAvailableDates() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        long spaceId = register(provider);
        LocalDate day = LocalDate.now().plusDays(5);

        mockMvc.perform(put("/api/v1/provider/spaces/{id}/available-dates", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dates\": [\"%s\", \"%s\"]}".formatted(day.plusDays(1), day)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableDates[0]").value(day.toString()))
                .andExpect(jsonPath("$.data.availableDates.length()").value(2));
    }

    /** 관리자 검수 API(9단계) 대신 상태만 바꿔 공개 상태를 만든다. */
    private void publish(long spaceId) {
        var space = spaceRepository.getSpace(spaceId);
        ReflectionTestUtils.setField(space, "status", SpaceStatus.PUBLISHED);
        spaceRepository.flush();
    }

    private long register(TestAccounts.Account provider) throws Exception {
        String body = mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SpaceFixtures.request(Set.of()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/id").asLong();
    }
}
