package com.dasisiheung.repository;

import com.dasisiheung.entity.privacy.PrivacyPurgeLog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 개인정보 파기 · 통계 집계용 일괄 SQL. 한 문장 안에서 표시와 집계를 함께 해 여러 번 실행해도 두 번 세지 않는다.
 * 파기 실행 기록(privacy_purge_log)의 저장소를 겸한다.
 */
public interface PrivacyRepository extends JpaRepository<PrivacyPurgeLog, Long> {

    /**
     * 이용이 끝난 확정 예약을 월별 통계에 더하고 표시한다.
     * 데이터를 바꾸는 CTE 를 SELECT 로 감싸 바뀐 건수를 돌려받는다. (쓰기 트랜잭션 안에서 불러야 한다)
     *
     * @return 더한 예약 수
     */
    @Query(value = """
            WITH counted AS (
                UPDATE space_application SET counted_in_stats = TRUE
                WHERE status = 'APPROVED' AND end_date < :today AND NOT counted_in_stats
                RETURNING space_id, activity_type, start_date, end_date, total_price
            ), inserted AS (
                INSERT INTO monthly_usage_stat (stat_month, region, activity_type, booking_count, booked_days, total_amount)
                SELECT CAST(date_trunc('month', c.start_date) AS DATE), s.region, c.activity_type,
                       COUNT(*), SUM(c.end_date - c.start_date + 1), SUM(c.total_price)
                FROM counted c JOIN space s ON s.id = c.space_id
                GROUP BY 1, 2, 3
                ON CONFLICT (stat_month, region, activity_type) DO UPDATE SET
                    booking_count = monthly_usage_stat.booking_count + EXCLUDED.booking_count,
                    booked_days = monthly_usage_stat.booked_days + EXCLUDED.booked_days,
                    total_amount = monthly_usage_stat.total_amount + EXCLUDED.total_amount
                RETURNING 1
            )
            SELECT COUNT(*) FROM counted
            """, nativeQuery = true)
    long countBookingsIntoStats(@Param("today") LocalDate today);

    /**
     * 보관 기간이 지난 끝난 신청의 글(활동 이름 · 계획 · 사유)과 가격 제안 한마디를 비식별화한다.
     * 통계에 들어가지 않은 확정 예약은 건너뛴다. (쓰기 트랜잭션 안에서 불러야 한다)
     *
     * @return 비식별화한 신청 수
     */
    @Query(value = """
            WITH anonymized AS (
                UPDATE space_application
                SET activity_title = '(파기됨)', plan = '(보관 기간이 지나 파기되었습니다)', status_reason = NULL,
                    anonymized_at = :now
                WHERE end_date < :cutoff AND anonymized_at IS NULL AND status <> 'PENDING'
                  AND (status <> 'APPROVED' OR counted_in_stats)
                RETURNING id
            ), offers AS (
                UPDATE price_offer SET message = NULL
                WHERE application_id IN (SELECT id FROM anonymized) AND message IS NOT NULL
                RETURNING 1
            )
            SELECT COUNT(*) FROM anonymized
            """, nativeQuery = true)
    long anonymizeApplicationsEndedBefore(@Param("cutoff") LocalDate cutoff, @Param("now") LocalDateTime now);

    @Query(value = """
            SELECT COUNT(*) FROM space_application
            WHERE end_date < :cutoff AND anonymized_at IS NULL AND status <> 'PENDING'
              AND (status <> 'APPROVED' OR counted_in_stats OR end_date < :today)
            """, nativeQuery = true)
    long countApplicationsToAnonymize(@Param("cutoff") LocalDate cutoff, @Param("today") LocalDate today);

    /** 마지막 메시지(없으면 생성 시각)가 기준보다 오래된 대화를 지운다. 메시지는 함께 지워진다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM conversation WHERE COALESCE(last_message_at, created_at) < :cutoff", nativeQuery = true)
    int deleteConversationsInactiveSince(@Param("cutoff") LocalDateTime cutoff);

    @Query(value = "SELECT COUNT(*) FROM conversation WHERE COALESCE(last_message_at, created_at) < :cutoff",
            nativeQuery = true)
    long countConversationsInactiveSince(@Param("cutoff") LocalDateTime cutoff);

    /** 이용 종료가 기준보다 오래된 예약의 활동 사진 저장 경로 (저장소 파일 삭제용). */
    @Query(value = """
            SELECT p.storage_path FROM activity_photo p JOIN space_application a ON a.id = p.application_id
            WHERE a.end_date < :cutoff
            """, nativeQuery = true)
    List<String> findActivityPhotoPathsEndedBefore(@Param("cutoff") LocalDate cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM activity_photo p USING space_application a
            WHERE a.id = p.application_id AND a.end_date < :cutoff
            """, nativeQuery = true)
    int deleteActivityPhotosEndedBefore(@Param("cutoff") LocalDate cutoff);
}
