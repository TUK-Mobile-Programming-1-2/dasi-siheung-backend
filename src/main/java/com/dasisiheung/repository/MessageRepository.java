package com.dasisiheung.repository;

import com.dasisiheung.entity.message.Message;
import com.dasisiheung.repository.projection.IdCount;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** 새 메시지 (폴링): afterId 보다 큰 ID, 오래된 순. */
    @EntityGraph(attributePaths = "sender")
    List<Message> findByConversation_IdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long afterId, Pageable pageable);

    /** 지난 메시지: beforeId 보다 작은 ID, 최근 순 (화면에는 뒤집어 보여준다). */
    @EntityGraph(attributePaths = "sender")
    List<Message> findByConversation_IdAndIdLessThanOrderByIdDesc(Long conversationId, Long beforeId, Pageable pageable);

    /** 대화별 안 읽은 메시지 수. 상대가 보냈고 내 읽음 위치보다 뒤인 메시지. */
    @Query("""
            select new com.dasisiheung.repository.projection.IdCount(m.conversation.id, count(m)) from Message m
            where m.conversation.id in :conversationIds and m.sender.id <> :memberId
              and m.id > case when m.conversation.user.id = :memberId
                              then m.conversation.userLastReadMessageId
                              else m.conversation.providerLastReadMessageId end
            group by m.conversation.id
            """)
    List<IdCount> countUnreadByConversation(@Param("conversationIds") Collection<Long> conversationIds,
                                             @Param("memberId") Long memberId);

    /** 내가 참여한 모든 대화의 안 읽은 메시지 수 (앱 배지). */
    @Query("""
            select count(m) from Message m
            where (m.conversation.user.id = :memberId or m.conversation.provider.id = :memberId)
              and m.sender.id <> :memberId
              and m.id > case when m.conversation.user.id = :memberId
                              then m.conversation.userLastReadMessageId
                              else m.conversation.providerLastReadMessageId end
            """)
    long countUnread(@Param("memberId") Long memberId);
}
