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
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.CommentRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.PostRepository;
import com.dasisiheung.util.LikePatterns;
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

    /**
     * @param category null 이면 전체
     * @param keyword  제목 · 본문 검색어 (선택)
     */
    public PageResponse<PostSummaryResponse> getPosts(PostCategory category, String keyword, PageQuery pageQuery) {
        String pattern = keyword == null || keyword.isBlank() ? null : LikePatterns.containsIgnoreCase(keyword.strip());
        return PageResponse.from(postRepository.search(ContentStatus.VISIBLE, category, pattern,
                pageQuery.toPageable(LATEST_FIRST)), PostSummaryResponse::from);
    }

    /** 상세를 열 때마다 조회 수가 1 오른다. */
    @Transactional
    public PostDetailResponse getPost(Long postId, Long viewerId) {
        getVisiblePost(postId);
        postRepository.increaseViewCount(postId);
        return PostDetailResponse.of(getVisiblePost(postId), viewerId);
    }

    @Transactional
    public PostDetailResponse write(Long authorId, PostRequest request) {
        Member author = getActiveMember(authorId);
        Post post = postRepository.save(Post.write(author, request.category(), request.title(), request.content()));
        return PostDetailResponse.of(post, authorId);
    }

    @Transactional
    public PostDetailResponse edit(Long memberId, Long postId, PostRequest request) {
        Post post = getVisiblePost(postId);
        post.edit(memberId, request.category(), request.title(), request.content());
        postRepository.flush();
        return PostDetailResponse.of(post, memberId);
    }

    /** 작성자 또는 관리자. 댓글도 함께 지워진다. */
    @Transactional
    public void delete(Long memberId, Role role, Long postId) {
        Post post = postRepository.getPost(postId);
        post.validateDeletableBy(memberId, role);
        postRepository.delete(post);
    }

    public PageResponse<CommentResponse> getComments(Long postId, PageQuery pageQuery) {
        getVisiblePost(postId);
        return PageResponse.from(commentRepository.findByPost_IdAndStatus(postId, ContentStatus.VISIBLE,
                pageQuery.toPageable(OLDEST_FIRST)), CommentResponse::from);
    }

    @Transactional
    public CommentResponse writeComment(Long authorId, Long postId, CommentRequest request) {
        Post post = getVisiblePost(postId);
        Comment comment = commentRepository.saveAndFlush(new Comment(post, getActiveMember(authorId), request.content()));
        CommentResponse response = CommentResponse.from(comment);
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

    private Post getVisiblePost(Long postId) {
        Post post = postRepository.getPost(postId);
        if (!post.isVisible()) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    private Member getActiveMember(Long memberId) {
        return memberRepository.findById(memberId)
                .filter(Member::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
