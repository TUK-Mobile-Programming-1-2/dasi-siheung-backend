package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.service.DemoDataInitializer;
import com.dasisiheung.support.TestDatabaseCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** demo 프로필: 시작할 때 시연 데이터를 만들고, 다시 실행해도 늘어나지 않는다. */
@SpringBootTest
@ActiveProfiles({"test", "demo"})
@ExtendWith(TestDatabaseCondition.class)
class DemoDataIntegrationTest {

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("시연 계정 · 공개 공간 · 후기(답글 대기 2개) · 180일 캘린더가 있고, 관리자는 만들지 않으며, 다시 실행해도 그대로다")
    void demoData() {
        assertThat(memberRepository.existsByEmail("provider1@demo.dasisiheung.com")).isTrue();
        int members = demoCount("SELECT count(*) FROM member WHERE email LIKE '%@demo.dasisiheung.com'");
        int published = demoCount("""
                SELECT count(*) FROM space s JOIN member m ON m.id = s.provider_id
                WHERE m.email LIKE '%@demo.dasisiheung.com' AND s.status = 'PUBLISHED'""");
        int reviews = demoCount("""
                SELECT count(*) FROM usage_review r JOIN member m ON m.id = r.reviewer_id
                WHERE m.email LIKE '%@demo.dasisiheung.com'""");

        assertThat(members).isEqualTo(7);
        assertThat(published).isEqualTo(5);
        assertThat(reviews).isEqualTo(5);
        if (createdJustNow()) {
            // 공유 개발 DB 처럼 예전 버전 시연 데이터가 이미 있으면 그 데이터는 건너뛴다 (다시 만들지 않으므로)
            assertThat(demoCount("""
                    SELECT count(*) FROM usage_review r JOIN member m ON m.id = r.reviewer_id
                    WHERE m.email LIKE '%@demo.dasisiheung.com' AND r.reply IS NULL""")).isEqualTo(2);
            assertThat(demoCount("""
                    SELECT count(*) FROM space_available_date d JOIN space s ON s.id = d.space_id
                    JOIN member m ON m.id = s.provider_id
                    WHERE m.email = 'provider1@demo.dasisiheung.com' AND d.available_date > current_date + 150"""))
                    .isPositive();
        }
        assertThat(demoCount("SELECT count(*) FROM member WHERE email LIKE '%@demo.dasisiheung.com' AND role = 'ADMIN'"))
                .isZero();

        demoDataInitializer.run(new DefaultApplicationArguments());

        assertThat(demoCount("SELECT count(*) FROM member WHERE email LIKE '%@demo.dasisiheung.com'")).isEqualTo(members);
    }

    /** 이번 테스트 실행에서 시연 데이터를 새로 만들었는지 (기준 계정의 가입 시각). */
    private boolean createdJustNow() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT created_at > now() - interval '30 minutes' FROM member
                WHERE email = 'provider1@demo.dasisiheung.com'""", Boolean.class));
    }

    private int demoCount(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
