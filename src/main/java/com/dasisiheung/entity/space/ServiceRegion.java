package com.dasisiheung.entity.space;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** 서비스 지역 경계 (Flyway 로만 바꾼다). 경계 판정은 ServiceRegionRepository 의 공간 쿼리가 맡는다. */
@Getter
@Entity
@Immutable
@Table(name = "service_region")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceRegion {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Region code;

    @Column(nullable = false, length = 30)
    private String name;
}
