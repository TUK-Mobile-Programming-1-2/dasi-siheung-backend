package com.dasisiheung.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer {accessToken} 헤더를 검증해 인증 정보를 채운다.
 * <p>
 * 토큰이 없거나 잘못돼도 여기서 응답하지 않는다. 공개 API 는 그대로 통과하고,
 * 보호된 API 는 {@link JsonAuthenticationEntryPoint} 가 기록된 사유(만료·위조)로 401 을 응답한다.
 * 서블릿 필터로 중복 등록되지 않도록 빈이 아닌 SecurityConfig 에서 직접 생성한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 토큰 검증 실패 사유(ErrorCode)를 엔트리 포인트로 넘기는 요청 속성 이름. */
    public static final String AUTH_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".AUTH_ERROR";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        resolveToken(request).ifPresent(token -> authenticate(request, token));
        filterChain.doFilter(request, response);
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).strip();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            AuthMember authMember = tokenProvider.parseAccessToken(token);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(authMember, null, authMember.authorities()));
            SecurityContextHolder.setContext(context);
        } catch (InvalidJwtException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
        }
    }
}
