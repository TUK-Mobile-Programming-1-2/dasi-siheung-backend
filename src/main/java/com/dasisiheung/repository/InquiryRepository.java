package com.dasisiheung.repository;

import com.dasisiheung.entity.inquiry.Inquiry;
import com.dasisiheung.entity.inquiry.InquiryStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    /** @throws BusinessException 없으면 INQUIRY_NOT_FOUND */
    default Inquiry getInquiry(Long inquiryId) {
        return findById(inquiryId).orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    /**
     * 사진 올리기용 행 잠금 (SELECT ... FOR UPDATE). 같은 문의에 동시에 올려도 3장을 넘지 않게 한다.
     * 3초 안에 잠금을 못 얻으면 409 DATA_CONFLICT 로 끝난다.
     *
     * @throws BusinessException 없으면 INQUIRY_NOT_FOUND
     */
    default Inquiry getInquiryWithLock(Long inquiryId) {
        return findWithLockById(inquiryId).orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select i from Inquiry i where i.id = :inquiryId")
    Optional<Inquiry> findWithLockById(@Param("inquiryId") Long inquiryId);

    /** 회원이 남긴 문의. */
    Page<Inquiry> findByMember_Id(Long memberId, Pageable pageable);

    /** @param status null 이면 전체 */
    @EntityGraph(attributePaths = "member")
    @Query(value = "select i from Inquiry i where (:status is null or i.status = :status)",
            countQuery = "select count(i) from Inquiry i where (:status is null or i.status = :status)")
    Page<Inquiry> search(@Param("status") InquiryStatus status, Pageable pageable);

    long countByStatus(InquiryStatus status);
}
