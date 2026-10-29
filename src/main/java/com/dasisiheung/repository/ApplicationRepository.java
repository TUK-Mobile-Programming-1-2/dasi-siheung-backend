package com.dasisiheung.repository;

import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.projection.EnumCount;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /** @throws BusinessException 없으면 APPLICATION_NOT_FOUND */
    default Application getApplication(Long applicationId) {
        return findWithSpaceAndApplicantById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    @EntityGraph(attributePaths = {"space", "space.provider", "applicant"})
    Optional<Application> findWithSpaceAndApplicantById(Long id);

    @EntityGraph(attributePaths = "space")
    Page<Application> findByApplicant_Id(Long applicantId, Pageable pageable);

    @EntityGraph(attributePaths = "space")
    Page<Application> findByApplicant_IdAndStatus(Long applicantId, ApplicationStatus status, Pageable pageable);

    /** 같은 신청자가 같은 공간에 겹치는 기간으로 낸 신청이 해당 상태로 있는지. */
    @Query("""
            select count(a) > 0 from Application a
            where a.space.id = :spaceId and a.applicant.id = :applicantId and a.status = :status
              and a.startDate <= :endDate and a.endDate >= :startDate
            """)
    boolean existsOverlappingByApplicant(@Param("spaceId") Long spaceId, @Param("applicantId") Long applicantId,
                                         @Param("status") ApplicationStatus status,
                                         @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 공간 제공자가 받은 신청.
     *
     * @param status  null 이면 전체
     * @param spaceId null 이면 내 모든 공간
     */
    @EntityGraph(attributePaths = {"space", "applicant"})
    @Query(value = """
            select a from Application a
            where a.space.provider.id = :providerId
              and (:status is null or a.status = :status)
              and (:spaceId is null or a.space.id = :spaceId)
            """, countQuery = """
            select count(a) from Application a
            where a.space.provider.id = :providerId
              and (:status is null or a.status = :status)
              and (:spaceId is null or a.space.id = :spaceId)
            """)
    Page<Application> findReceived(@Param("providerId") Long providerId, @Param("status") ApplicationStatus status,
                                   @Param("spaceId") Long spaceId, Pageable pageable);

    @Query("select a.space.id from Application a where a.id = :applicationId")
    Optional<Long> findSpaceIdById(@Param("applicationId") Long applicationId);

    /** 같은 공간에 기간이 겹치는 해당 상태의 신청이 있는지. */
    @Query("""
            select count(a) > 0 from Application a
            where a.space.id = :spaceId and a.status = :status
              and a.startDate <= :endDate and a.endDate >= :startDate
            """)
    boolean existsOverlapping(@Param("spaceId") Long spaceId, @Param("status") ApplicationStatus status,
                              @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /** 같은 공간에 기간이 겹치는 해당 상태의 신청들. */
    @Query("""
            select a from Application a
            where a.space.id = :spaceId and a.status = :status
              and a.startDate <= :endDate and a.endDate >= :startDate
            """)
    List<Application> findOverlapping(@Param("spaceId") Long spaceId, @Param("status") ApplicationStatus status,
                                      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /** 공간의 확정 예약 중 from 이후에 끝나는 것 (이용자 캘린더에서 뺄 날짜). */
    @Query("""
            select a from Application a
            where a.space.id = :spaceId and a.status = com.dasisiheung.entity.application.ApplicationStatus.APPROVED
              and a.endDate >= :from
            """)
    List<Application> findApprovedEndingFrom(@Param("spaceId") Long spaceId, @Param("from") LocalDate from);

    @Query("select new com.dasisiheung.repository.projection.EnumCount(a.status, count(a)) from Application a group by a.status")
    List<EnumCount<ApplicationStatus>> countByStatus();
}
