package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.community.AuthorInfo;
import com.dasisiheung.dto.community.CommentResponse;
import com.dasisiheung.dto.community.PostDetailResponse;
import com.dasisiheung.dto.community.PostPhotoResponse;
import com.dasisiheung.dto.community.PostSummaryResponse;
import com.dasisiheung.entity.community.Comment;
import com.dasisiheung.entity.community.Post;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.repository.PostPhotoRepository;
import com.dasisiheung.repository.projection.PostPhotoPath;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/** 게시글 · 댓글 엔티티를 응답으로 바꾼다. 글쓴이 프로필 사진과 게시글 사진 주소를 채운다. */
@Component
@RequiredArgsConstructor
public class CommunityResponseMapper {

    private final PostPhotoRepository postPhotoRepository;
    private final FileUploadService fileUploadService;
    private final ProfileImageResolver profileImageResolver;

    /** 목록. 썸네일은 페이지의 글들을 한 번에 조회한다. (글마다 쿼리하지 않음) */
    public PageResponse<PostSummaryResponse> summaries(Page<Post> posts) {
        List<Long> postIds = posts.map(Post::getId).getContent();
        Map<Long, String> thumbnails = postIds.isEmpty() ? Map.of()
                : postPhotoRepository.findThumbnails(postIds).stream()
                        .collect(Collectors.toMap(PostPhotoPath::postId, path -> photoUrl(path.storagePath())));
        return PageResponse.from(posts,
                post -> PostSummaryResponse.of(post, author(post.getAuthor()), thumbnails.get(post.getId())));
    }

    /** @param viewerId 비로그인이면 null */
    public PostDetailResponse detail(Post post, Long viewerId) {
        return PostDetailResponse.of(post, author(post.getAuthor()), photos(post.getId()), viewerId);
    }

    /** 올린 순. */
    public List<PostPhotoResponse> photos(Long postId) {
        return postPhotoRepository.findByPost_IdOrderByIdAsc(postId).stream()
                .map(photo -> PostPhotoResponse.of(photo, photoUrl(photo.getStoragePath())))
                .toList();
    }

    public CommentResponse comment(Comment comment) {
        return CommentResponse.of(comment, author(comment.getAuthor()));
    }

    private AuthorInfo author(Member member) {
        return AuthorInfo.of(member, profileImageResolver.urlOf(member));
    }

    private String photoUrl(String storagePath) {
        return fileUploadService.url(UploadPolicy.POST_PHOTO, storagePath);
    }
}
