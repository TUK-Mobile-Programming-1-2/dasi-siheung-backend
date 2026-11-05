package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.space.EquipmentItem;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.space.Equipment;
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
import java.util.List;
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
    @DisplayName("수용 인원 · 비품 · 가격 협의: 등록 → 응답 · DB 순서 그대로 → 수정하면 비품 통째로 교체 → 잘못된 값 400")
    void capacityEquipmentNegotiation() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        var request = SpaceFixtures.request(Set.of(tomorrow));

        String body = mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.maxCapacity").value(10))
                .andExpect(jsonPath("$.data.priceNegotiable").value(true))
                .andExpect(jsonPath("$.data.equipment[0].name").value("접이식 테이블"))
                .andExpect(jsonPath("$.data.equipment[1].quantity").value(8))
                .andReturn().getResponse().getContentAsString();
        long spaceId = objectMapper.readTree(body).at("/data/id").asLong();

        var changed = SpaceFixtures.withExtras(request, request.title(), null,
                List.of(new EquipmentItem("빔프로젝터", 1), new EquipmentItem("의자", 12), new EquipmentItem("행거", 2)),
                false);
        mockMvc.perform(put("/api/v1/provider/spaces/{id}", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxCapacity").doesNotExist())
                .andExpect(jsonPath("$.data.priceNegotiable").value(false))
                .andExpect(jsonPath("$.data.equipment.length()").value(3));

        entityManager.flush();
        entityManager.clear();
        Space saved = spaceRepository.findById(spaceId).orElseThrow();
        assertThat(saved.getMaxCapacity()).isNull();
        assertThat(saved.isPriceNegotiable()).isFalse();
        assertThat(saved.getEquipment()).containsExactly(
                new Equipment("빔프로젝터", 1), new Equipment("의자", 12), new Equipment("행거", 2));

        var duplicated = SpaceFixtures.withExtras(request, request.title(), 10,
                List.of(new EquipmentItem("의자", 4), new EquipmentItem(" 의자", 2)), true);
        var zeroCapacity = SpaceFixtures.withExtras(request, request.title(), 0, null, null);
        for (var invalid : List.of(duplicated, zeroCapacity)) {
            mockMvc.perform(put("/api/v1/provider/spaces/{id}", spaceId)
                            .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        }
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
