package com.dasisiheung.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.locationtech.jts.geom.Point;

class GeoPointsTest {

    private static final double GEOBUKSEOM_LATITUDE = 37.3175;
    private static final double GEOBUKSEOM_LONGITUDE = 126.6822;

    @Test
    @DisplayName("위도·경도를 (x=경도, y=위도) 순서의 WGS84 Point 로 만든다")
    void createsWgs84Point() {
        Point point = GeoPoints.of(GEOBUKSEOM_LATITUDE, GEOBUKSEOM_LONGITUDE);

        assertThat(point.getX()).isEqualTo(GEOBUKSEOM_LONGITUDE);
        assertThat(point.getY()).isEqualTo(GEOBUKSEOM_LATITUDE);
        assertThat(point.getSRID()).isEqualTo(GeoPoints.WGS84_SRID);
    }

    @ParameterizedTest(name = "위도 {0}, 경도 {1}")
    @CsvSource({
            "90.0001, 126.0",
            "-90.0001, 126.0",
            "37.0, 180.0001",
            "37.0, -180.0001",
            "NaN, 126.0"
    })
    @DisplayName("범위를 벗어난 좌표는 거부한다")
    void rejectsOutOfRange(double latitude, double longitude) {
        assertThatIllegalArgumentException().isThrownBy(() -> GeoPoints.of(latitude, longitude));
    }

    @ParameterizedTest(name = "위도 {0}, 경도 {1}")
    @CsvSource({"90, 180", "-90, -180"})
    @DisplayName("경계값 좌표는 허용한다")
    void acceptsBoundary(double latitude, double longitude) {
        assertThat(GeoPoints.of(latitude, longitude).isValid()).isTrue();
    }
}
