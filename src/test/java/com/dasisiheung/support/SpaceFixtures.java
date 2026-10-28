package com.dasisiheung.support;

import com.dasisiheung.dto.space.SpaceRequest;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.BuildingUse;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Facility;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.test.util.ReflectionTestUtils;

/** 공간 테스트 데이터. 좌표는 각 서비스 지역 경계 안의 실제 지점이다. */
public final class SpaceFixtures {

    /** 정왕역 앞 */
    public static final double JEONGWANG_LAT = 37.3517;
    public static final double JEONGWANG_LNG = 126.7428;
    /** 거북섬 */
    public static final double GEOBUKSEOM_LAT = 37.3175;
    public static final double GEOBUKSEOM_LNG = 126.6822;
    /** 배곧 생명공원 부근 */
    public static final double BAEGOT_LAT = 37.3773;
    public static final double BAEGOT_LNG = 126.7302;
    /** 서울시청 (서비스 지역 밖) */
    public static final double SEOUL_LAT = 37.5665;
    public static final double SEOUL_LNG = 126.9780;

    private SpaceFixtures() {
    }

    public static SpaceRequest request(double latitude, double longitude, Set<LocalDate> availableDates) {
        return new SpaceRequest("정왕역 앞 1층 상가", "유동 인구가 많은 역 앞 코너 상가입니다.", "21시 이후 소음 금지",
                "경기도 시흥시 마유로 418", "정왕프라자", "101호", latitude, longitude, new BigDecimal("33.06"),
                BuildingUse.NEIGHBORHOOD_1, CommercialTheme.FOOD_ALLEY, 50_000,
                Set.of(Facility.WIFI, Facility.ELECTRICITY), Set.of(ActivityType.POPUP_STORE, ActivityType.EXHIBITION),
                availableDates);
    }

    public static SpaceRequest request(Set<LocalDate> availableDates) {
        return request(JEONGWANG_LAT, JEONGWANG_LNG, availableDates);
    }

    public static Member provider(long id) {
        Member provider = Member.signUp("provider" + id + "@example.com", "hash", "제공자", "01012345678", Role.PROVIDER);
        ReflectionTestUtils.setField(provider, "id", id);
        return provider;
    }

    /** 저장된 것처럼 ID 와 생성 시각을 채운 공간. */
    public static Space space(long id, Member provider, Set<LocalDate> availableDates, LocalDate today) {
        Space space = Space.register(provider, Region.JEONGWANG, request(availableDates).toDetails(), availableDates, today);
        ReflectionTestUtils.setField(space, "id", id);
        ReflectionTestUtils.setField(space, "version", 0L);
        ReflectionTestUtils.setField(space, "createdAt", FixedClockConfig.FIXED_LOCAL_NOW);
        ReflectionTestUtils.setField(space, "updatedAt", FixedClockConfig.FIXED_LOCAL_NOW);
        return space;
    }
}
