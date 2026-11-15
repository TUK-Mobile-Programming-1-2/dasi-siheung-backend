package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.member.BlockedMemberResponse;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.MemberBlock;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberBlockRepository;
import com.dasisiheung.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 차단. 차단 · 해제 모두 여러 번 호출해도 결과가 같다(멱등).
 * 차단 관계에서 막히는 일(메시지 전송 · 새 대화)은 {@link ConversationService} 가 확인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberBlockService {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;
    private final ProfileImageResolver profileImageResolver;
    private final Clock clock;

    /**
     * 이미 차단했으면 그대로 두고 처음 차단한 시각을 돌려준다. 정지된 회원도 차단할 수 있다.
     *
     * @throws BusinessException 없거나 탈퇴한 회원이면 MEMBER_NOT_FOUND, 자기 자신이면 CANNOT_BLOCK_SELF
     */
    @Transactional
    public BlockedMemberResponse block(Long blockerId, Long targetId) {
        Member target = memberRepository.findById(targetId)
                .filter(member -> !member.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        MemberBlock block = memberBlockRepository.findByBlocker_IdAndBlocked_Id(blockerId, targetId)
                .orElseGet(() -> memberBlockRepository.save(MemberBlock.block(
                        memberRepository.getReferenceById(blockerId), target, LocalDateTime.now(clock))));
        return toResponse(block);
    }

    /** 차단하지 않은 상대여도 성공한다. 탈퇴한 회원도 해제할 수 있다. */
    @Transactional
    public void unblock(Long blockerId, Long targetId) {
        memberBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, targetId);
    }

    /** 내가 차단한 회원 (최근 차단 순). 탈퇴한 회원도 남아 해제할 수 있다. */
    public PageResponse<BlockedMemberResponse> getMyBlocks(Long blockerId, PageQuery pageQuery) {
        return PageResponse.from(memberBlockRepository.findByBlocker_Id(blockerId, pageQuery.toPageable(LATEST_FIRST)),
                this::toResponse);
    }

    private BlockedMemberResponse toResponse(MemberBlock block) {
        return BlockedMemberResponse.of(block, profileImageResolver.urlOf(block.getBlocked()));
    }
}
