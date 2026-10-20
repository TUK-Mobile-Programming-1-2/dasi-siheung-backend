package com.dasisiheung.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 전체 컨텍스트 + 실제 PostgreSQL/PostGIS 통합 테스트.
 * <p>
 * 접속 정보는 {@code TEST_DB_URL / TEST_DB_USERNAME / TEST_DB_PASSWORD} 환경 변수로 받는다.
 * 같은 설정을 쓰는 테스트끼리 스프링 컨텍스트를 공유한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(TestDatabaseCondition.class)
public @interface IntegrationTest {
}
