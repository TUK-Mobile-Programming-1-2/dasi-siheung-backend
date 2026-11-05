package com.dasisiheung.repository;

import com.dasisiheung.entity.message.Conversation;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.projection.ConversationLink;
import java.util.Collection;
import java.util.List;
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

    @Query("select c.id from Conversation c where c.space.id = :spaceId and c.user.id = :userId")
    Optional<Long> findIdBySpaceIdAndUserId(@Param("spaceId") Long spaceId, @Param("userId") Long userId);

    /**
     * 목록의 신청들에 연결된 대화를 한 번에 찾는다. 공간 · 이용자 ID 를 각각 IN 으로 거르므로
     * 호출하는 쪽이 (공간, 이용자) 쌍으로 다시 맞춰 쓴다.
     */
    @Query("""
            select new com.dasisiheung.repository.projection.ConversationLink(c.id, c.space.id, c.user.id)
            from Conversation c
            where c.space.id in :spaceIds and c.user.id in :userIds
            """)
    List<ConversationLink> findLinks(@Param("spaceIds") Collection<Long> spaceIds,
                                     @Param("userIds") Collection<Long> userIds);

    /** 내가 이용자 또는 제공자로 참여한 대화 (최근 메시지 순, 메시지 없는 대화는 맨 뒤). */
    @EntityGraph(attributePaths = {"space", "user", "provider"})
    @Query(value = """
            select c from Conversation c
            where c.user.id = :memberId or c.provider.id = :memberId
            order by c.lastMessageAt desc nulls last, c.id desc
            """, countQuery = "select count(c) from Conversation c where c.user.id = :memberId or c.provider.id = :memberId")
    Page<Conversation> findMine(@Param("memberId") Long memberId, Pageable pageable);
}
