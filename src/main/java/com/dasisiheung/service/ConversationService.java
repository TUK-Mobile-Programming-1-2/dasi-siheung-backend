package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.message.ConversationResponse;
import com.dasisiheung.dto.message.MessageResponse;
import com.dasisiheung.dto.message.UnreadCountResponse;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.message.Conversation;
import com.dasisiheung.entity.message.Message;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.ConversationRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.MessageRepository;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.repository.projection.IdCount;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공간별 1:1 소통. 실시간 연결 대신 앱이 주기적으로 새 메시지를 조회(폴링)한다.
 * <ul>
 *   <li>대화방 화면: 5초마다 {@code after=마지막 메시지 ID} 로 새 메시지 조회</li>
 *   <li>탭 배지: 30초마다 안 읽은 수 조회</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationService {

    /** 한 번에 내려주는 메시지 수 상한. */
    public static final int MAX_MESSAGES = 100;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SpaceRepository spaceRepository;
    private final MemberRepository memberRepository;
    private final ApplicationRepository applicationRepository;
    private final SpaceResponseMapper spaceResponseMapper;
    private final Clock clock;

    /**
     * 이용자가 공간 제공자에게 대화를 연다. 이미 있으면 그 대화를 돌려준다(멱등).
     *
     * @throws BusinessException 새로 열 때 공개 공간이 아니면 SPACE_NOT_FOUND
     */
    @Transactional
    public ConversationResponse open(Long userId, Long spaceId) {
        Conversation conversation = conversationRepository.findBySpace_IdAndUser_Id(spaceId, userId)
                .orElseGet(() -> conversationRepository.save(new Conversation(getPublishedSpace(spaceId),
                        memberRepository.getReferenceById(userId))));
        return toResponse(conversation, userId, 0);
    }

    /**
     * 신청에서 1:1 대화를 연다. 신청자 · 공간 제공자 누구나 열 수 있고, 이미 있으면 그 대화를 돌려준다(멱등).
     * 대화는 공간 · 이용자 한 쌍에 하나라, 같은 공간에 여러 번 신청해도 같은 대화로 이어진다.
     * 신청이 있는 공간이면 공개 상태와 관계없이 열 수 있다. (일시 중단 중에도 예약 관련 연락은 필요)
     *
     * @throws BusinessException 신청이 없으면 APPLICATION_NOT_FOUND, 참여자가 아니면 NOT_APPLICATION_PARTICIPANT
     */
    @Transactional
    public ConversationResponse openForApplication(Long memberId, Long applicationId) {
        Application application = applicationRepository.getApplication(applicationId);
        application.validateParticipant(memberId);
        Conversation conversation = conversationRepository
                .findBySpace_IdAndUser_Id(application.getSpace().getId(), application.getApplicant().getId())
                .orElseGet(() -> conversationRepository.save(
                        new Conversation(application.getSpace(), application.getApplicant())));
        return toResponse(conversation, memberId, unreadCount(conversation, memberId));
    }

    public PageResponse<ConversationResponse> getMyConversations(Long memberId, PageQuery pageQuery) {
        Page<Conversation> conversations = conversationRepository.findMine(memberId, pageQuery.toPageable());
        List<Long> ids = conversations.map(Conversation::getId).getContent();
        Map<Long, Long> unread = ids.isEmpty() ? Map.of()
                : messageRepository.countUnreadByConversation(ids, memberId).stream()
                        .collect(Collectors.toMap(IdCount::id, IdCount::count));
        return PageResponse.from(conversations,
                conversation -> toResponse(conversation, memberId, unread.getOrDefault(conversation.getId(), 0L)));
    }

    /**
     * 메시지 조회 (오래된 순).
     * <ul>
     *   <li>after: 그 ID 뒤의 새 메시지 (폴링)</li>
     *   <li>before: 그 ID 앞의 지난 메시지 (위로 스크롤)</li>
     *   <li>둘 다 없으면 최근 메시지</li>
     * </ul>
     */
    public List<MessageResponse> getMessages(Long memberId, Long conversationId, Long after, Long before, int size) {
        getParticipatingConversation(memberId, conversationId);
        PageRequest limit = PageRequest.of(0, Math.min(size, MAX_MESSAGES));
        List<Message> messages;
        if (after != null) {
            messages = messageRepository.findByConversation_IdAndIdGreaterThanOrderByIdAsc(conversationId, after, limit);
        } else {
            long upperBound = before == null ? Long.MAX_VALUE : before;
            messages = messageRepository.findByConversation_IdAndIdLessThanOrderByIdDesc(conversationId, upperBound, limit)
                    .reversed();
        }
        return messages.stream().map(message -> MessageResponse.of(message, memberId)).toList();
    }

    @Transactional
    public MessageResponse send(Long memberId, Long conversationId, String content) {
        Conversation conversation = getParticipatingConversation(memberId, conversationId);
        Member sender = memberRepository.findById(memberId)
                .filter(Member::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Message message = messageRepository.saveAndFlush(
                new Message(conversation, sender, content, LocalDateTime.now(clock)));
        conversation.onMessageSent(message);
        return MessageResponse.of(message, memberId);
    }

    @Transactional
    public ConversationResponse markRead(Long memberId, Long conversationId, long lastMessageId) {
        Conversation conversation = getParticipatingConversation(memberId, conversationId);
        conversation.markRead(memberId, lastMessageId);
        conversationRepository.flush();
        return toResponse(conversation, memberId, unreadCount(conversation, memberId));
    }

    public UnreadCountResponse getUnreadCount(Long memberId) {
        return new UnreadCountResponse(messageRepository.countUnread(memberId));
    }

    private Conversation getParticipatingConversation(Long memberId, Long conversationId) {
        Conversation conversation = conversationRepository.getConversation(conversationId);
        conversation.validateParticipant(memberId);
        return conversation;
    }

    private long unreadCount(Conversation conversation, Long memberId) {
        if (conversation.getId() == null) {
            return 0;
        }
        return messageRepository.countUnreadByConversation(List.of(conversation.getId()), memberId).stream()
                .mapToLong(IdCount::count).sum();
    }

    private Space getPublishedSpace(Long spaceId) {
        return spaceRepository.findByIdAndStatus(spaceId, SpaceStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
    }

    private ConversationResponse toResponse(Conversation conversation, Long viewerId, long unreadCount) {
        return ConversationResponse.of(conversation, viewerId,
                spaceResponseMapper.thumbnailUrl(conversation.getSpace()), unreadCount);
    }
}
