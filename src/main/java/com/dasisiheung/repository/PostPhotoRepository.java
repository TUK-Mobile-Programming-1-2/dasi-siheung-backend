package com.dasisiheung.repository;

import com.dasisiheung.entity.community.PostPhoto;
import com.dasisiheung.repository.projection.PostPhotoPath;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostPhotoRepository extends JpaRepository<PostPhoto, Long> {

    /** 올린 순. */
    List<PostPhoto> findByPost_IdOrderByIdAsc(Long postId);

    int countByPost_Id(Long postId);

    Optional<PostPhoto> findByIdAndPost_Id(Long id, Long postId);

    /** 목록 썸네일: 글마다 처음 올린 사진 하나를 한 번에 찾는다. 사진 없는 글은 결과에 없다. */
    @Query("""
            select new com.dasisiheung.repository.projection.PostPhotoPath(p.post.id, p.storagePath)
            from PostPhoto p
            where p.post.id in :postIds
              and p.id = (select min(first.id) from PostPhoto first where first.post.id = p.post.id)
            """)
    List<PostPhotoPath> findThumbnails(@Param("postIds") Collection<Long> postIds);

    /** 글을 지울 때 저장소에서 함께 지울 파일들. (행은 DB 가 ON DELETE CASCADE 로 지운다) */
    @Query("select p.storagePath from PostPhoto p where p.post.id = :postId")
    List<String> findStoragePathsByPostId(@Param("postId") Long postId);
}
