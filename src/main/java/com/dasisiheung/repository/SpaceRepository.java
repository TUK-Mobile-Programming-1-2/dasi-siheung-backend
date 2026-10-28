package com.dasisiheung.repository;

import com.dasisiheung.entity.space.Space;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    /** @throws BusinessException 없으면 SPACE_NOT_FOUND */
    default Space getSpace(Long spaceId) {
        return findById(spaceId).orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
    }
}
