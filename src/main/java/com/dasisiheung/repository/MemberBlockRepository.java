package com.dasisiheung.repository;

import com.dasisiheung.entity.member.MemberBlock;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberBlockRepository extends JpaRepository<MemberBlock, Long> {

    /**
     * 누가 차단했는지는 알리지 않는다. (차단당한 사실이 드러나지 않도록 같은 오류)
     *
     * @throws BusinessException 두 회원 중 한 명이라도 상대를 차단했으면 BLOCKED_MEMBER
     */
    default void validateNotBlocked(Long memberId, Long otherMemberId) {
        if (existsBetween(memberId, otherMemberId)) {
            throw new BusinessException(ErrorCode.BLOCKED_MEMBER);
        }
    }

    /** 두 회원 중 한 명이라도 상대를 차단했는지. */
    @Query("""
            select count(b) > 0 from MemberBlock b
            where (b.blocker.id = :memberId and b.blocked.id = :otherMemberId)
               or (b.blocker.id = :otherMemberId and b.blocked.id = :memberId)
            """)
    boolean existsBetween(@Param("memberId") Long memberId, @Param("otherMemberId") Long otherMemberId);

    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    Optional<MemberBlock> findByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    /** 내가 차단한 회원 (차단 목록). */
    @EntityGraph(attributePaths = "blocked")
    Page<MemberBlock> findByBlocker_Id(Long blockerId, Pageable pageable);

    /** 주어진 회원들 가운데 내가 차단한 회원 ID. 대화 목록의 상대들을 한 번에 확인한다. (대화마다 쿼리하지 않음) */
    @Query("select b.blocked.id from MemberBlock b where b.blocker.id = :blockerId and b.blocked.id in :memberIds")
    List<Long> findBlockedIdsAmong(@Param("blockerId") Long blockerId, @Param("memberIds") Collection<Long> memberIds);

    /** @return 지운 행 수 (0 이면 차단하지 않았던 것) */
    @Modifying
    @Query("delete from MemberBlock b where b.blocker.id = :blockerId and b.blocked.id = :blockedId")
    int deleteByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);
}
