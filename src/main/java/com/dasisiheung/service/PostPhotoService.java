package com.dasisiheung.service;

import com.dasisiheung.dto.community.PostPhotoResponse;
import com.dasisiheung.entity.community.Post;
import com.dasisiheung.entity.community.PostPhoto;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.PostPhotoRepository;
import com.dasisiheung.repository.PostRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 게시글 사진 추가 · 삭제. 글은 JSON 으로 먼저 쓰고, 받은 글 ID 로 사진을 올린다.
 * 업로드 중 오류로 트랜잭션이 롤백되면 이미 올린 파일은 FileUploadService 가 지운다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostPhotoService {

    private static final String DIRECTORY = "community/posts/%d/photos";

    private final PostRepository postRepository;
    private final PostPhotoRepository photoRepository;
    private final MemberRepository memberRepository;
    private final FileUploadService fileUploadService;
    private final CommunityResponseMapper responseMapper;
    private final Clock clock;

    /**
     * 사진을 올린 순서대로 뒤에 더하고 전체 사진 목록을 돌려준다.
     *
     * @throws BusinessException 파일이 없으면 FILE_REQUIRED, 작성자가 아니면 NOT_POST_AUTHOR,
     *                           합쳐서 5장을 넘으면 POST_PHOTO_LIMIT_EXCEEDED (파일을 올리기 전에 검사)
     */
    @Transactional
    public List<PostPhotoResponse> addPhotos(Long memberId, Long postId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_REQUIRED);
        }
        memberRepository.getActiveMember(memberId);
        Post post = postRepository.getVisiblePost(postId);
        post.validateCanAddPhotos(memberId, photoRepository.countByPost_Id(postId), files.size());

        String directory = DIRECTORY.formatted(postId);
        LocalDateTime now = LocalDateTime.now(clock);
        for (MultipartFile file : files) {
            StoredFile stored = fileUploadService.upload(UploadPolicy.POST_PHOTO, directory, file);
            photoRepository.save(new PostPhoto(post, stored.path(), now));
        }
        photoRepository.flush();
        return responseMapper.photos(postId);
    }

    /**
     * 작성자만 지울 수 있다. 파일은 커밋된 뒤 저장소에서 지운다.
     *
     * @throws BusinessException 작성자가 아니면 NOT_POST_AUTHOR, 이 글의 사진이 아니면 POST_PHOTO_NOT_FOUND
     */
    @Transactional
    public void deletePhoto(Long memberId, Long postId, Long photoId) {
        Post post = postRepository.getVisiblePost(postId);
        post.validateCanDeletePhoto(memberId);
        PostPhoto photo = photoRepository.findByIdAndPost_Id(photoId, postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_PHOTO_NOT_FOUND));
        photoRepository.delete(photo);
        fileUploadService.deleteAfterCommit(UploadPolicy.POST_PHOTO, photo.getStoragePath());
    }
}
