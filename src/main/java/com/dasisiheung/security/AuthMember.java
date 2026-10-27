package com.dasisiheung.security;

import com.dasisiheung.entity.member.Role;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 액세스 토큰에서 꺼낸 로그인 회원 정보. 컨트롤러에서 {@link LoginMember} 로 주입받는다.
 * <p>
 * 역할은 토큰 발급 시점 기준이다. 관리자로 승격된 회원은 다시 로그인해야 새 역할이 반영된다.
 */
public record AuthMember(Long id, Role role) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }
}
