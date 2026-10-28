package com.dasisiheung.repository;

import com.dasisiheung.entity.space.SpaceReview;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceReviewRepository extends JpaRepository<SpaceReview, Long> {

    @EntityGraph(attributePaths = "admin")
    List<SpaceReview> findBySpace_IdOrderByCreatedAtDescIdDesc(Long spaceId);
}
