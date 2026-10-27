package com.dasisiheung.support;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.security.JwtTokenProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link ApiDocsTest} 테스트의 공통 기반. 실제 JWT 를 발급해 Authorization 헤더 예시까지 문서에 남긴다.
 */
public abstract class ApiDocsTestSupport {

    protected static final long USER_ID = 1L;
    protected static final long PROVIDER_ID = 2L;
    protected static final long ADMIN_ID = 3L;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    protected String bearer(long memberId, Role role) {
        return "Bearer " + jwtTokenProvider.createAccessToken(memberId, role);
    }

    protected String userToken() {
        return bearer(USER_ID, Role.USER);
    }

    protected String providerToken() {
        return bearer(PROVIDER_ID, Role.PROVIDER);
    }

    protected String adminToken() {
        return bearer(ADMIN_ID, Role.ADMIN);
    }

    protected String json(Object body) throws JsonProcessingException {
        return objectMapper.writeValueAsString(body);
    }
}
