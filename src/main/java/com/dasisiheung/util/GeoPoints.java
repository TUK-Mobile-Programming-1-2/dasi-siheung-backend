package com.dasisiheung.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * 위도·경도를 PostGIS 에 저장할 WGS84(SRID 4326) {@link Point} 로 만든다.
 * JTS 좌표는 (x = 경도, y = 위도) 순서라 혼동하기 쉬우므로 생성은 이 클래스로만 한다.
 */
public final class GeoPoints {

    public static final int WGS84_SRID = 4326;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);
    private static final double MAX_LATITUDE = 90.0;
    private static final double MAX_LONGITUDE = 180.0;

    private GeoPoints() {
    }

    public static Point of(double latitude, double longitude) {
        validateRange("위도", latitude, MAX_LATITUDE);
        validateRange("경도", longitude, MAX_LONGITUDE);
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private static void validateRange(String name, double value, double limit) {
        boolean inRange = value >= -limit && value <= limit;
        if (!inRange) {
            throw new IllegalArgumentException("%s는 -%s ~ %s 사이여야 합니다: %s".formatted(name, limit, limit, value));
        }
    }
}
