package com.dasisiheung.service;

import com.dasisiheung.entity.community.Comment;
import com.dasisiheung.entity.community.Post;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.report.ReportTargetType;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.CommentRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.PostRepository;
import com.dasisiheung.repository.SpaceRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 신고 대상(공간 · 게시글 · 댓글 · 회원)을 찾고 숨긴다. 대상 종류별 분기를 한곳에 모은다. */
@Component
@RequiredArgsConstructor
public class ReportTargetResolver {

    private static final int SUMMARY_LENGTH = 100;

    private final SpaceRepository spaceRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;

    /** @throws BusinessException 대상이 없으면 REPORT_TARGET_NOT_FOUND */
    public ReportTarget find(ReportTargetType type, Long id) {
        return switch (type) {
            case SPACE -> {
                Space space = spaceRepository.findById(id).orElseThrow(ReportTargetResolver::notFound);
                yield new ReportTarget(type, id, space.getProvider(), space.getTitle(),
                        space.getStatus() != SpaceStatus.HIDDEN);
            }
            case POST -> {
                Post post = postRepository.findById(id).orElseThrow(ReportTargetResolver::notFound);
                yield new ReportTarget(type, id, post.getAuthor(), post.getTitle(), post.isVisible());
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(id).orElseThrow(ReportTargetResolver::notFound);
                yield new ReportTarget(type, id, comment.getAuthor(), summarize(comment.getContent()), comment.isVisible());
            }
            case MEMBER -> {
                Member member = memberRepository.findById(id).orElseThrow(ReportTargetResolver::notFound);
                yield new ReportTarget(type, id, member, member.getName(), true);
            }
        };
    }

    /** 대상 요약 한 줄. 대상이 지워졌으면 null. */
    public String summaryOf(ReportTargetType type, Long id) {
        try {
            return find(type, id).summary();
        } catch (BusinessException deleted) {
            return null;
        }
    }

    /**
     * 대상을 숨긴다. 이미 숨겨졌으면 아무것도 하지 않는다.
     * 댓글 수 갱신은 영속성 컨텍스트를 비우므로 다른 변경을 마친 뒤 마지막에 부른다.
     */
    public void hide(ReportTargetType type, Long id, String reason, LocalDateTime now) {
        switch (type) {
            case SPACE -> spaceRepository.findById(id).ifPresent(space -> space.hide(reason, now));
            case POST -> postRepository.findById(id).ifPresent(Post::hide);
            case COMMENT -> commentRepository.findById(id).filter(Comment::isVisible).ifPresent(comment -> {
                comment.hide();
                postRepository.addCommentCount(comment.getPost().getId(), -1);
            });
            case MEMBER -> throw new BusinessException(ErrorCode.INVALID_REPORT_ACTION);
        }
    }

    private static String summarize(String content) {
        return content.length() <= SUMMARY_LENGTH ? content : content.substring(0, SUMMARY_LENGTH) + "…";
    }

    private static BusinessException notFound() {
        return new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND);
    }
}
