package com.dasisiheung.repository;

import com.dasisiheung.entity.report.Report;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.entity.report.ReportTargetType;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /** @throws BusinessException 없으면 REPORT_NOT_FOUND */
    default Report getReport(Long reportId) {
        return findById(reportId).orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
    }

    boolean existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(Long reporterId, ReportTargetType targetType,
                                                                 Long targetId, ReportStatus status);

    /**
     * @param status     null 이면 전체
     * @param targetType null 이면 전체
     */
    @EntityGraph(attributePaths = "reporter")
    @Query(value = """
            select r from Report r
            where (:status is null or r.status = :status) and (:targetType is null or r.targetType = :targetType)
            """, countQuery = """
            select count(r) from Report r
            where (:status is null or r.status = :status) and (:targetType is null or r.targetType = :targetType)
            """)
    Page<Report> search(@Param("status") ReportStatus status, @Param("targetType") ReportTargetType targetType,
                        Pageable pageable);

    List<Report> findByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, Long targetId, ReportStatus status);

    long countByStatus(ReportStatus status);
}
