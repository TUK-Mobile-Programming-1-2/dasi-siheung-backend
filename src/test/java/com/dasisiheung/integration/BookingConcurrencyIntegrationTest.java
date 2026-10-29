package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.ProviderApplicationService;
import com.dasisiheung.support.ApplicationScenario;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 같은 공간 · 같은 기간의 신청 10건을 동시에 승인해도 확정은 정확히 1건이어야 한다.
 * 실제 커밋이 필요해 테스트 트랜잭션을 쓰지 않는다. (만든 데이터는 고유 계정이라 다른 테스트와 섞이지 않는다)
 */
@IntegrationTest
class BookingConcurrencyIntegrationTest {

    private static final int THREADS = 10;
    private static final LocalDate DAY1 = LocalDate.now(ClockConfig.SERVICE_ZONE).plusDays(1);
    private static final LocalDate DAY2 = DAY1.plusDays(1);

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Autowired
    private ApplicationScenario applicationScenario;

    @Autowired
    private ProviderApplicationService providerApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("동시 승인 10건 → 확정 1건, 나머지는 409 (이미 확정 · 마감) 이고 중복 확정은 0건")
    void concurrentApprovals() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of(DAY1, DAY2));
        List<Long> applicationIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            applicationIds.add(applicationScenario.submit(testAccounts.create(Role.USER), spaceId, DAY1, DAY2));
        }

        AtomicInteger approved = new AtomicInteger();
        Map<String, AtomicInteger> failures = new ConcurrentHashMap<>();
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        for (Long applicationId : applicationIds) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    providerApplicationService.approve(provider.id(), applicationId);
                    approved.incrementAndGet();
                } catch (BusinessException e) {
                    failures.computeIfAbsent(e.getErrorCode().name(), key -> new AtomicInteger()).incrementAndGet();
                } catch (Exception e) {
                    failures.computeIfAbsent(e.getClass().getSimpleName(), key -> new AtomicInteger()).incrementAndGet();
                }
                return null;
            });
        }
        ready.await();
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(approved.get()).isEqualTo(1);
        assertThat(failures.keySet()).isSubsetOf(
                ErrorCode.PERIOD_ALREADY_BOOKED.name(), ErrorCode.INVALID_APPLICATION_STATUS.name());
        assertThat(countByStatus(spaceId, "APPROVED")).isEqualTo(1);
        assertThat(countByStatus(spaceId, "CLOSED")).isEqualTo(THREADS - 1);
    }

    @Test
    @DisplayName("애플리케이션 검사를 우회해도 DB 배타 제약이 겹치는 확정을 막는다 (23P01)")
    void exclusionConstraint() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of(DAY1, DAY2));
        long first = applicationScenario.submit(testAccounts.create(Role.USER), spaceId, DAY1, DAY2);
        long second = applicationScenario.submit(testAccounts.create(Role.USER), spaceId, DAY2, DAY2);

        jdbcTemplate.update("UPDATE space_application SET status = 'APPROVED' WHERE id = ?", first);

        assertThatThrownBy(() ->
                jdbcTemplate.update("UPDATE space_application SET status = 'APPROVED' WHERE id = ?", second))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ex_space_application_no_double_booking");
    }

    private Integer countByStatus(long spaceId, String status) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM space_application WHERE space_id = ? AND status = ?", Integer.class, spaceId, status);
    }
}
