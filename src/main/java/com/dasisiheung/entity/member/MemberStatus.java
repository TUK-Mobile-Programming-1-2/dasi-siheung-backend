package com.dasisiheung.entity.member;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberStatus implements LabeledEnum {

    ACTIVE("정상"),
    SUSPENDED("이용 정지"),
    WITHDRAWN("탈퇴");

    private final String label;
}
