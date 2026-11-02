package com.dasisiheung.service;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.report.ReportTargetType;

/**
 * 신고 대상 요약.
 *
 * @param owner   대상의 작성자 · 공간 제공자 (회원 신고면 그 회원) — 이용 정지 대상
 * @param summary 관리자 화면에 보여줄 한 줄 (공간 이름 · 글 제목 · 댓글 앞부분 · 회원 이름)
 * @param visible 아직 숨겨지지 않았는지 (회원은 항상 true)
 */
public record ReportTarget(ReportTargetType type, Long id, Member owner, String summary, boolean visible) {
}
