package com.dasisiheung.entity.member;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원 역할. 관리자는 직접 가입할 수 없고 DBA 가 DB 에서 승격한다. (docs/ADMIN_GUIDE.md)
 */
@Getter
@RequiredArgsConstructor
public enum Role implements LabeledEnum {

    USER("이용자", true),
    PROVIDER("공간 제공자", true),
    ADMIN("관리자", false);

    private final String label;
    private final boolean selfRegistrable;

    /** Spring Security 권한 이름 (hasRole('PROVIDER') 와 대응). */
    public String authority() {
        return "ROLE_" + name();
    }
}
