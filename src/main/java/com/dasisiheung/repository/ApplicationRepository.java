package com.dasisiheung.repository;

import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import java.time.LocalDate;
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
}
