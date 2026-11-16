package com.dasisiheung.repository;

import com.dasisiheung.entity.inquiry.InquiryPhoto;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryPhotoRepository extends JpaRepository<InquiryPhoto, Long> {

    /** 올린 순. */
    List<InquiryPhoto> findByInquiry_IdOrderByIdAsc(Long inquiryId);

    int countByInquiry_Id(Long inquiryId);

    /** 문의별 사진 수. 사진이 없는 문의는 결과에 없다. */
    default Map<Long, Long> countByInquiryIds(Collection<Long> inquiryIds) {
        if (inquiryIds.isEmpty()) {
            return Map.of();
        }
        return countRowsByInquiryIds(inquiryIds).stream()
                .collect(Collectors.toMap(PhotoCount::inquiryId, PhotoCount::count));
    }

    @Query("""
            select new com.dasisiheung.repository.InquiryPhotoRepository$PhotoCount(p.inquiry.id, count(p))
            from InquiryPhoto p where p.inquiry.id in :inquiryIds group by p.inquiry.id
            """)
    List<PhotoCount> countRowsByInquiryIds(@Param("inquiryIds") Collection<Long> inquiryIds);

    record PhotoCount(Long inquiryId, long count) {
    }
}
