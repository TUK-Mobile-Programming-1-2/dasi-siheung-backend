package com.dasisiheung.repository;

import com.dasisiheung.entity.report.ReportPhoto;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportPhotoRepository extends JpaRepository<ReportPhoto, Long> {

    /** 올린 순. */
    List<ReportPhoto> findByReport_IdOrderByIdAsc(Long reportId);

    /** 신고별 사진 수. 사진이 없는 신고는 결과에 없다. */
    default Map<Long, Long> countByReportIds(Collection<Long> reportIds) {
        if (reportIds.isEmpty()) {
            return Map.of();
        }
        return countRowsByReportIds(reportIds).stream()
                .collect(Collectors.toMap(PhotoCount::reportId, PhotoCount::count));
    }

    @Query("""
            select new com.dasisiheung.repository.ReportPhotoRepository$PhotoCount(p.report.id, count(p))
            from ReportPhoto p where p.report.id in :reportIds group by p.report.id
            """)
    List<PhotoCount> countRowsByReportIds(@Param("reportIds") Collection<Long> reportIds);

    record PhotoCount(Long reportId, long count) {
    }
}
