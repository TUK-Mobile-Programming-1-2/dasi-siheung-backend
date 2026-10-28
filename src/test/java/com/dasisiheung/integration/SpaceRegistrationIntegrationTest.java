package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceFixtures;
import com.dasisiheung.support.TestAccounts;
import com.dasisiheung.util.GeoPoints;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 실제 PostGIS 로 지역 판정(ST_Covers)과 좌표·시설·날짜 저장을 확인한다. */
@IntegrationTest
@Transactional
class SpaceRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest(name = "{2}")
    @CsvSource({
            "37.3517, 126.7428, JEONGWANG",
            "37.3175, 126.6822, GEOBUKSEOM",
            "37.3773, 126.7302, BAEGOT"
    })
    @DisplayName("좌표가 속한 서비스 지역으로 판정해 검수 대기 상태로 저장한다")
    void registersWithRegion(double latitude, double longitude, String region) throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        String body = mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                SpaceFixtures.request(latitude, longitude, Set.of(tomorrow)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.region").value(region))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();

        Long spaceId = objectMapper.readTree(body).at("/data/id").asLong();
        entityManager.flush();
        entityManager.clear();
        Space saved = spaceRepository.findById(spaceId).orElseThrow();
        assertThat(saved.getLocation().getSRID()).isEqualTo(GeoPoints.WGS84_SRID);
        assertThat(saved.latitude()).isEqualTo(latitude);
        assertThat(saved.getStatus()).isEqualTo(SpaceStatus.PENDING_REVIEW);
        assertThat(saved.getAvailableDates()).containsExactly(tomorrow);
        assertThat(saved.getActivityTypes()).hasSize(2);
        assertThat(saved.isOwnedBy(provider.id())).isTrue();
    }

    @Test
    @DisplayName("서비스 지역 밖(서울시청) 좌표는 400 SPACE_OUT_OF_SERVICE_AREA")
    void rejectsOutsideServiceArea() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);

        mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                SpaceFixtures.request(SpaceFixtures.SEOUL_LAT, SpaceFixtures.SEOUL_LNG, Set.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SPACE_OUT_OF_SERVICE_AREA"));
    }
}
