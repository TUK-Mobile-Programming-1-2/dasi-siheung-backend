package com.dasisiheung.repository;

import com.dasisiheung.entity.space.Favorite;
import com.dasisiheung.entity.space.SpaceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMember_IdAndSpace_Id(Long memberId, Long spaceId);

    /** @return 지운 행 수 (0 이면 찜하지 않았던 것) */
    @Modifying
    @Query("delete from Favorite f where f.member.id = :memberId and f.space.id = :spaceId")
    int deleteByMemberIdAndSpaceId(@Param("memberId") Long memberId, @Param("spaceId") Long spaceId);

    /** 내 찜 목록 중 해당 상태의 공간 (보통 공개 공간만). */
    @EntityGraph(attributePaths = "space")
    Page<Favorite> findByMember_IdAndSpace_Status(Long memberId, SpaceStatus status, Pageable pageable);
}
