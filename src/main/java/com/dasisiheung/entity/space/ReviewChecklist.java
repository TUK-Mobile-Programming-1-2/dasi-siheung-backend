package com.dasisiheung.entity.space;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 검수 5개 항목 — 등록 정보와 제출 증빙이 일치하는지.
 * 공개 승인은 5개 항목이 모두 확인되어야 한다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReviewChecklist {

    /** 제공 권한: 공간을 제공할 권한이 있는지 증빙으로 확인 */
    @Column(name = "authority_verified", nullable = false)
    private boolean authorityVerified;

    /** 주소: 등록 주소와 증빙 일치 */
    @Column(name = "address_matched", nullable = false)
    private boolean addressMatched;

    /** 건물·호실: 등록 건물·호실과 증빙 일치 */
    @Column(name = "building_unit_matched", nullable = false)
    private boolean buildingUnitMatched;

    /** 면적: 등록 면적과 증빙 일치 */
    @Column(name = "area_matched", nullable = false)
    private boolean areaMatched;

    /** 용도: 등록 용도와 증빙 일치 */
    @Column(name = "usage_matched", nullable = false)
    private boolean usageMatched;

    public boolean allPassed() {
        return authorityVerified && addressMatched && buildingUnitMatched && areaMatched && usageMatched;
    }
}
