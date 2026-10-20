package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.dasisiheung.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class PostgisMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway V1 마이그레이션이 성공 기록된다")
    void flywayMigrationSucceeded() {
        Boolean succeeded = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class);

        assertThat(succeeded).isTrue();
    }

    @Test
    @DisplayName("PostGIS 확장이 활성화되어 있다")
    void postgisIsEnabled() {
        String version = jdbcTemplate.queryForObject(
                "SELECT extversion FROM pg_extension WHERE extname = 'postgis'", String.class);

        assertThat(version).isNotBlank();
    }

    @Test
    @DisplayName("WGS84 좌표 사이 거리를 미터 단위로 계산한다 (경도 0.004° ≈ 354m, 위도 37.34°)")
    void calculatesGeographyDistanceInMeters() {
        Double meters = jdbcTemplate.queryForObject("""
                SELECT ST_Distance(
                    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                )
                """, Double.class, 126.7335, 37.3417, 126.7375, 37.3417);

        assertThat(meters).isBetween(330.0, 380.0);
    }
}
