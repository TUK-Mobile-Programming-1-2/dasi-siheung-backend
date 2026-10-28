package com.dasisiheung.repository;

import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.ServiceRegion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceRegionRepository extends JpaRepository<ServiceRegion, Region> {

    /** 좌표를 포함하는(경계선 포함) 서비스 지역 코드. 경계가 겹치면 코드 순으로 하나만 고른다. */
    @Query(value = """
            SELECT code
            FROM service_region
            WHERE ST_Covers(boundary, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326))
            ORDER BY code
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findRegionCodeContaining(@Param("latitude") double latitude, @Param("longitude") double longitude);
}
