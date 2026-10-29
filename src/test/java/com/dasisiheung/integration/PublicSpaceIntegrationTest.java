package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Facility;
import com.dasisiheung.support.SpaceFixtures;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 로그인 없이 공간을 둘러보는 흐름. */
@IntegrationTest
@Transactional
class PublicSpaceIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ClockConfig.SERVICE_ZONE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Test
    @DisplayName("공개된 공간은 토큰 없이 상세를 볼 수 있고, 일시 중단하면 404 가 된다")
    void detail() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        long spaceId = spaceScenario.publishedSpace(provider, admin, Set.of(TODAY.plusDays(2), TODAY.plusDays(1)));

        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(spaceId))
                .andExpect(jsonPath("$.data.availableDates[0]").value(TODAY.plusDays(1).toString()))
                .andExpect(jsonPath("$.data.provider.id").value(provider.id()))
                .andExpect(jsonPath("$.data.proofs").doesNotExist())
                .andExpect(jsonPath("$.data.reviewComment").doesNotExist());

        mockMvc.perform(post("/api/v1/provider/spaces/{id}/pause", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SPACE_NOT_FOUND"));
    }

    @Test
    @DisplayName("검수 대기 공간은 이용자에게 보이지 않는다")
    void pendingSpaceHidden() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        long spaceId = spaceScenario.register(provider, Set.of());

        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("공간 둘러보기는 조회만 공개다 — 쓰기 요청은 401")
    void writeRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/v1/spaces/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("검색: 지역 · 활동 · 목적 · 기간 · 예산 · 시설 조건을 모두 만족하는 공개 공간만 나온다")
    void search() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        String token = "검색" + UUID.randomUUID().toString().substring(0, 8);
        LocalDate day1 = TODAY.plusDays(1);
        LocalDate day2 = TODAY.plusDays(2);
        long cheap = spaceScenario.publishedSpace(provider, admin, SpaceFixtures.request(token + " 정왕 먹자골목",
                SpaceFixtures.JEONGWANG_LAT, SpaceFixtures.JEONGWANG_LNG, CommercialTheme.FOOD_ALLEY, 40_000,
                Set.of(Facility.WIFI, Facility.ELECTRICITY), Set.of(ActivityType.POPUP_STORE),
                Set.of(day1, day2, TODAY.plusDays(3))));
        long cafe = spaceScenario.publishedSpace(provider, admin, SpaceFixtures.request(token + " 배곧 카페거리",
                SpaceFixtures.BAEGOT_LAT, SpaceFixtures.BAEGOT_LNG, CommercialTheme.CAFE_STREET, 90_000,
                Set.of(Facility.WIFI), Set.of(ActivityType.EXHIBITION), Set.of(day1)));
        spaceScenario.register(provider, SpaceFixtures.request(token + " 검수 대기", SpaceFixtures.JEONGWANG_LAT,
                SpaceFixtures.JEONGWANG_LNG, CommercialTheme.FOOD_ALLEY, 10_000, Set.of(),
                Set.of(ActivityType.POPUP_STORE), Set.of(day1)));

        search(token, "sort", "PRICE_ASC")
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(cheap))
                .andExpect(jsonPath("$.data.content[1].id").value(cafe));
        expectOnly(search(token, "region", "BAEGOT"), cafe);
        expectOnly(search(token, "activityType", "POPUP_STORE"), cheap);
        expectOnly(search(token, "purpose", "PROMOTION"), cafe);
        expectOnly(search(token, "startDate", day1.toString(), "endDate", day2.toString()), cheap);
        expectOnly(search(token, "maxPrice", "50000"), cheap);
        expectOnly(search(token, "minPrice", "50000"), cafe);
        expectOnly(search(token, "facilities", "WIFI", "facilities", "ELECTRICITY"), cheap);
    }

    @Test
    @DisplayName("검색: 지난 기간은 400 INVALID_SEARCH_PERIOD, 종료일 없는 기간은 400 INVALID_INPUT")
    void invalidPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/spaces")
                        .param("startDate", TODAY.minusDays(1).toString()).param("endDate", TODAY.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SEARCH_PERIOD"));

        mockMvc.perform(get("/api/v1/spaces").param("startDate", TODAY.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    private ResultActions search(String keyword, String... params) throws Exception {
        var request = get("/api/v1/spaces").param("keyword", keyword).param("size", "100");
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request).andExpect(status().isOk());
    }

    private static void expectOnly(ResultActions result, long spaceId) throws Exception {
        result.andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(spaceId));
    }

    @Test
    @DisplayName("지도: 화면 영역 안의 공개 공간만 마커로 나온다")
    void markers() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        long geobukseom = spaceScenario.publishedSpace(provider, admin, SpaceFixtures.request("거북섬 지도 테스트",
                SpaceFixtures.GEOBUKSEOM_LAT, SpaceFixtures.GEOBUKSEOM_LNG, CommercialTheme.WATERFRONT, 60_000,
                Set.of(), Set.of(ActivityType.EXHIBITION), Set.of()));
        long jeongwang = spaceScenario.publishedSpace(provider, admin, Set.of());

        mockMvc.perform(get("/api/v1/spaces/map")
                        .param("swLat", "37.300").param("swLng", "126.650")
                        .param("neLat", "37.330").param("neLng", "126.700"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markers[?(@.id == %d)].region".formatted(geobukseom)).value("GEOBUKSEOM"))
                .andExpect(jsonPath("$.data.markers[?(@.id == %d)]".formatted(jeongwang)).isEmpty());

        mockMvc.perform(get("/api/v1/spaces/map")
                        .param("swLat", "37.300").param("swLng", "126.650")
                        .param("neLat", "37.330").param("neLng", "126.700")
                        .param("activityType", "POPUP_STORE"))
                .andExpect(jsonPath("$.data.markers[?(@.id == %d)]".formatted(geobukseom)).isEmpty());
    }
}
