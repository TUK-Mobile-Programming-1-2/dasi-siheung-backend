package com.dasisiheung.repository;

import com.dasisiheung.entity.application.ActivityPhoto;
import com.dasisiheung.entity.application.PhotoPhase;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityPhotoRepository extends JpaRepository<ActivityPhoto, Long> {

    /** 올린 순. (시점 정렬은 enum 선언 순서라 서비스가 한다) */
    @EntityGraph(attributePaths = "uploader")
    List<ActivityPhoto> findByApplication_IdOrderByIdAsc(Long applicationId);

    int countByApplication_IdAndPhase(Long applicationId, PhotoPhase phase);

    Optional<ActivityPhoto> findByIdAndApplication_Id(Long id, Long applicationId);
}
