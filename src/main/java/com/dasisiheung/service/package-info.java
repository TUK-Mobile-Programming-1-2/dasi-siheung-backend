/**
 * 서비스 계층 — 비즈니스 규칙과 트랜잭션 경계.
 * <ul>
 *   <li>컨트롤러에게만 호출되고, 리포지토리를 사용한다.</li>
 *   <li>규칙 위반은 {@link com.dasisiheung.exception.BusinessException} 으로 알린다.</li>
 *   <li>예약 승인처럼 여러 테이블을 함께 바꾸는 작업은 하나의 트랜잭션으로 묶는다.</li>
 * </ul>
 */
package com.dasisiheung.service;
