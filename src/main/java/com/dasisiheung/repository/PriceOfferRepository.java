package com.dasisiheung.repository;

import com.dasisiheung.entity.application.PriceOffer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceOfferRepository extends JpaRepository<PriceOffer, Long> {

    /** 협의 이력 (오래된 순). */
    @EntityGraph(attributePaths = "proposer")
    List<PriceOffer> findByApplication_IdOrderByIdAsc(Long applicationId);

    Optional<PriceOffer> findFirstByApplication_IdOrderByIdDesc(Long applicationId);
}
