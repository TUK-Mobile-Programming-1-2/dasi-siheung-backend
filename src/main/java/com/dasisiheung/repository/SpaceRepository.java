package com.dasisiheung.repository;

import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.projection.EnumCount;
import com.dasisiheung.repository.projection.SpaceMarker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceRepository extends JpaRepository<Space, Long>, JpaSpecificationExecutor<Space> {

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

    /**
     * 지도 화면 영역(경계 사각형) 안의 공개 공간 마커. GiST 인덱스(&&)를 탄다.
     * 선택 조건은 null 대신 값으로 받는다: activityType '' = 전체, maxPrice = Integer.MAX_VALUE 면 제한 없음.
     * 별칭은 따옴표로 감싸 대소문자를 지킨다 (PostgreSQL 은 감싸지 않은 별칭을 소문자로 바꿔 projection 이 못 찾는다).
     */
    @Query(value = """
            SELECT s.id AS "id", s.title AS "title", s.region AS "region", s.daily_price AS "dailyPrice",
                   ST_Y(s.location) AS "latitude", ST_X(s.location) AS "longitude"
            FROM space s
            WHERE s.status = 'PUBLISHED'
              AND s.location && ST_MakeEnvelope(:west, :south, :east, :north, 4326)
              AND s.daily_price <= :maxPrice
              AND (:activityType = '' OR EXISTS (
                    SELECT 1 FROM space_activity a WHERE a.space_id = s.id AND a.activity_type = :activityType))
            ORDER BY s.reviewed_at DESC, s.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<SpaceMarker> findMarkersWithin(@Param("south") double south, @Param("west") double west,
                                        @Param("north") double north, @Param("east") double east,
                                        @Param("activityType") String activityType, @Param("maxPrice") int maxPrice,
                                        @Param("limit") int limit);
}
