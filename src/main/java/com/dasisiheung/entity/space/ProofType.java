package com.dasisiheung.entity.space;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 공간 제공 권한 증빙 종류. */
@Getter
@RequiredArgsConstructor
public enum ProofType implements LabeledEnum {

    REGISTRY_CERTIFICATE("등기사항증명서"),
    LEASE_CONTRACT("임대차계약서"),
    SUBLEASE_CONSENT("전대 동의서"),
    POWER_OF_ATTORNEY("위임장"),
    BUSINESS_REGISTRATION("사업자등록증"),
    OTHER("기타");

    private final String label;
}
