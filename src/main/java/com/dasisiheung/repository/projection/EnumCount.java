package com.dasisiheung.repository.projection;

/**
 * group by 집계 결과 한 줄 (JPQL 생성자 표현식용).
 *
 * @param key   집계 기준 enum (상태·지역·역할 등)
 * @param count 개수
 */
public record EnumCount<E extends Enum<E>>(E key, Long count) {
}
