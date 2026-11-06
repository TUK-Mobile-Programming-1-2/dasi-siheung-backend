package com.dasisiheung.repository.projection;

/**
 * 게시글별 사진 경로 (JPQL 생성자 표현식용).
 *
 * @param postId      게시글 ID
 * @param storagePath 저장소 경로
 */
public record PostPhotoPath(Long postId, String storagePath) {
}
