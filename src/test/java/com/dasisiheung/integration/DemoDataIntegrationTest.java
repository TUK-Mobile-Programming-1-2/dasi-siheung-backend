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
    @DisplayName("시연 계정 · 공개 공간 · 후기 · 통계가 있고, 관리자는 만들지 않으며, 다시 실행해도 그대로다")
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
        assertThat(demoCount("SELECT count(*) FROM member WHERE email LIKE '%@demo.dasisiheung.com' AND role = 'ADMIN'"))
                .isZero();

        demoDataInitializer.run(new DefaultApplicationArguments());

        assertThat(demoCount("SELECT count(*) FROM member WHERE email LIKE '%@demo.dasisiheung.com'")).isEqualTo(members);
    }

    private int demoCount(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
