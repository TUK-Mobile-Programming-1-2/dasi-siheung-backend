package com.dasisiheung.repository;

import com.dasisiheung.entity.message.Conversation;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** @throws BusinessException 없으면 CONVERSATION_NOT_FOUND */
    default Conversation getConversation(Long conversationId) {
        return findById(conversationId).orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    Optional<Conversation> findBySpace_IdAndUser_Id(Long spaceId, Long userId);

    /** 내가 이용자 또는 제공자로 참여한 대화 (최근 메시지 순, 메시지 없는 대화는 맨 뒤). */
    @EntityGraph(attributePaths = {"space", "user", "provider"})
    @Query(value = """
            select c from Conversation c
            where c.user.id = :memberId or c.provider.id = :memberId
            order by c.lastMessageAt desc nulls last, c.id desc
            """, countQuery = "select count(c) from Conversation c where c.user.id = :memberId or c.provider.id = :memberId")
    Page<Conversation> findMine(@Param("memberId") Long memberId, Pageable pageable);
}
