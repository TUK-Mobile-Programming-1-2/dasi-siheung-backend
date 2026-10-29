package com.dasisiheung.repository.projection;

/** 지도 마커용 최소 정보 (네이티브 쿼리 결과). 지역 코드는 문자열로 받는다. */
public interface SpaceMarker {

    Long getId();

    String getTitle();

    String getRegion();

    Integer getDailyPrice();

    Double getLatitude();

    Double getLongitude();
}
