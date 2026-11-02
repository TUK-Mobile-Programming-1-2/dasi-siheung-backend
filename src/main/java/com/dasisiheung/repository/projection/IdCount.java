package com.dasisiheung.repository.projection;

/**
 * 묶음별 개수 (JPQL 생성자 표현식용).
 *
 * @param id    묶음 기준 ID (대화 ID 등)
 * @param count 개수
 */
public record IdCount(Long id, Long count) {
}
