package com.dasisiheung.repository;

import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    /** @throws BusinessException 없으면 SPACE_NOT_FOUND */
    default Space getSpace(Long spaceId) {
        return findById(spaceId).orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
    }

    Page<Space> findByProvider_Id(Long providerId, Pageable pageable);

    Page<Space> findByProvider_IdAndStatus(Long providerId, SpaceStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "provider")
    Page<Space> findByStatus(SpaceStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "provider")
    Page<Space> findAllBy(Pageable pageable);
}
