package com.dasisiheung.support;

import com.dasisiheung.config.SecurityConfig;
import com.dasisiheung.security.JsonAccessDeniedHandler;
import com.dasisiheung.security.JsonAuthenticationEntryPoint;
import com.dasisiheung.security.JwtTokenProvider;
import com.dasisiheung.security.SecurityErrorResponseWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

/**
 * 컨트롤러 슬라이스 + Spring REST Docs 테스트. DB 없이 빠르게 실행된다.
 * <p>
 * 이 테스트가 만든 스니펫으로 {@code ./gradlew generateApiDocs} 가 openapi3.yaml 을 만든다.
 * 실제 보안 설정(JWT 필터 포함)을 함께 올려 문서와 운영 동작(인증 여부)이 어긋나지 않게 한다.
 * 서비스는 {@code @MockitoBean} 으로 대체하고, 테스트 클래스는 {@link ApiDocsTestSupport} 를 상속한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@AutoConfigureRestDocs
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        JwtTokenProvider.class,
        FixedClockConfig.class,
        RestDocsConfig.class
})
public @interface ApiDocsTest {

    /** 테스트할 컨트롤러. */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};
}
