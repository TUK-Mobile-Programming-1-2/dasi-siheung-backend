package com.dasisiheung.security;

import com.dasisiheung.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증 정보 없이 보호된 API 에 접근하면 401 을 응답한다.
 * 토큰이 만료됐으면 EXPIRED_TOKEN, 위조·손상됐으면 INVALID_TOKEN, 토큰이 없으면 UNAUTHORIZED 코드를 내려준다.
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object authError = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        ErrorCode errorCode = authError instanceof ErrorCode code ? code : ErrorCode.UNAUTHORIZED;
        errorResponseWriter.write(response, errorCode);
    }
}
