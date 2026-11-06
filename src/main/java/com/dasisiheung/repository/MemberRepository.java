package com.dasisiheung.repository;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.MemberStatus;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.projection.EnumCount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /** @throws BusinessException 없거나 정상 상태(정지 · 탈퇴가 아닌)가 아니면 MEMBER_NOT_FOUND */
    default Member getActiveMember(Long memberId) {
        return findById(memberId)
                .filter(Member::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    @Query("""
            select new com.dasisiheung.repository.projection.EnumCount(m.role, count(m))
            from Member m where m.status <> :excluded group by m.role
            """)
    List<EnumCount<Role>> countByRoleExcludingStatus(@Param("excluded") MemberStatus excluded);
}
