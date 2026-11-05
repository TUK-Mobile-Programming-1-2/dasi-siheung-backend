package com.dasisiheung.repository.projection;

/**
 * 신청과 대화를 잇는 키 (JPQL 생성자 표현식용). 대화는 공간 · 이용자 한 쌍에 하나다.
 *
 * @param conversationId 대화 ID
 * @param spaceId        공간 ID
 * @param userId         이용자(신청자) 회원 ID
 */
public record ConversationLink(Long conversationId, Long spaceId, Long userId) {
}
