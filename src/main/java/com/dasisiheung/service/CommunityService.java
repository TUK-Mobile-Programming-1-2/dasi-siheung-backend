package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.community.CommentRequest;
import com.dasisiheung.dto.community.CommentResponse;
import com.dasisiheung.dto.community.PostDetailResponse;
import com.dasisiheung.dto.community.PostRequest;
import com.dasisiheung.dto.community.PostSummaryResponse;
import com.dasisiheung.entity.community.Comment;
import com.dasisiheung.entity.community.ContentStatus;
import com.dasisiheung.entity.community.Post;
import com.dasisiheung.entity.community.PostCategory;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.repository.CommentRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.PostPhotoRepository;
import com.dasisiheung.repository.PostRepository;
import com.dasisiheung.util.LikePatterns;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 게시글 · 댓글. 조회는 누구나, 쓰기는 로그인 회원, 공지는 관리자만.
 * 숨김(신고 처리)된 글 · 댓글은 보이지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");
    private static final Sort OLDEST_FIRST = Sort.by(Sort.Direction.ASC, "createdAt", "id");

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final PostPhotoRepository postPhotoRepository;
    private final FileUploadService fileUploadService;
    private final CommunityResponseMapper responseMapper;

    /**
     * @param category null 이면 전체
     * @param keyword  제목 · 본문 검색어 (선택)
     */
    public PageResponse<PostSummaryResponse> getPosts(PostCategory category, String keyword, PageQuery pageQuery) {
        String pattern = keyword == null || keyword.isBlank() ? null : LikePatterns.containsIgnoreCase(keyword.strip());
        return responseMapper.summaries(postRepository.search(ContentStatus.VISIBLE, category, pattern,
                pageQuery.toPageable(LATEST_FIRST)));
    }

    /** 상세를 열 때마다 조회 수가 1 오른다. */
    @Transactional
    public PostDetailResponse getPost(Long postId, Long viewerId) {
        postRepository.getVisiblePost(postId);
        postRepository.increaseViewCount(postId);
        return responseMapper.detail(postRepository.getVisiblePost(postId), viewerId);
    }

    @Transactional
    public PostDetailResponse write(Long authorId, PostRequest request) {
        Member author = memberRepository.getActiveMember(authorId);
        Post post = postRepository.save(Post.write(author, request.category(), request.title(), request.content()));
        return responseMapper.detail(post, authorId);
    }

    @Transactional
    public PostDetailResponse edit(Long memberId, Long postId, PostRequest request) {
        Post post = postRepository.getVisiblePost(postId);
        post.edit(memberId, request.category(), request.title(), request.content());
        postRepository.flush();
        return responseMapper.detail(post, memberId);
    }

    /** 작성자 또는 관리자. 댓글 · 사진도 함께 지워진다. (사진 파일은 커밋된 뒤 저장소에서 지운다) */
    @Transactional
    public void delete(Long memberId, Role role, Long postId) {
        Post post = postRepository.getPost(postId);
        post.validateDeletableBy(memberId, role);
        List<String> photoPaths = postPhotoRepository.findStoragePathsByPostId(postId);
        postRepository.delete(post);
        photoPaths.forEach(path -> fileUploadService.deleteAfterCommit(UploadPolicy.POST_PHOTO, path));
    }

    public PageResponse<CommentResponse> getComments(Long postId, PageQuery pageQuery) {
        postRepository.getVisiblePost(postId);
        return PageResponse.from(commentRepository.findByPost_IdAndStatus(postId, ContentStatus.VISIBLE,
                pageQuery.toPageable(OLDEST_FIRST)), responseMapper::comment);
    }

    @Transactional
    public CommentResponse writeComment(Long authorId, Long postId, CommentRequest request) {
        Post post = postRepository.getVisiblePost(postId);
        Member author = memberRepository.getActiveMember(authorId);
        Comment comment = commentRepository.saveAndFlush(new Comment(post, author, request.content()));
        CommentResponse response = responseMapper.comment(comment);
        postRepository.addCommentCount(postId, 1);
        return response;
    }

    /** 작성자 또는 관리자. */
    @Transactional
    public void deleteComment(Long memberId, Role role, Long commentId) {
        Comment comment = commentRepository.getComment(commentId);
        comment.validateDeletableBy(memberId, role);
        Long postId = comment.getPost().getId();
        boolean counted = comment.isVisible();
        commentRepository.delete(comment);
        commentRepository.flush();
        if (counted) {
            postRepository.addCommentCount(postId, -1);
        }
    }
}
