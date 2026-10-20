/**
 * 리포지토리 계층 — Spring Data JPA 인터페이스와 PostGIS 공간 쿼리.
 * <ul>
 *   <li>서비스 계층에게만 호출된다.</li>
 *   <li>반경 검색처럼 미터 단위가 필요한 쿼리는 {@code ::geography} 로 변환해 계산한다.</li>
 * </ul>
 */
package com.dasisiheung.repository;
