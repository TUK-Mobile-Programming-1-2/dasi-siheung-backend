package com.dasisiheung.repository;

import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.projection.EnumCount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    /** @throws BusinessException 없으면 SPACE_NOT_FOUND */
    default Space getSpace(Long spaceId) {
        return findById(spaceId).orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
    }

    /** 이용자에게 보이는 공간 조회용. 상태가 다르면 비어 있다. */
    @EntityGraph(attributePaths = "provider")
    Optional<Space> findByIdAndStatus(Long id, SpaceStatus status);

    Page<Space> findByProvider_Id(Long providerId, Pageable pageable);

    Page<Space> findByProvider_IdAndStatus(Long providerId, SpaceStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "provider")
    Page<Space> findByStatus(SpaceStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "provider")
    Page<Space> findAllBy(Pageable pageable);

    @Query("select new com.dasisiheung.repository.projection.EnumCount(s.status, count(s)) from Space s group by s.status")
    List<EnumCount<SpaceStatus>> countByStatus();

    @Query("""
            select new com.dasisiheung.repository.projection.EnumCount(s.region, count(s))
            from Space s where s.status = :status group by s.region
            """)
    List<EnumCount<Region>> countByRegion(@Param("status") SpaceStatus status);

    List<Space> findTop5ByOrderByCreatedAtDesc();
}
