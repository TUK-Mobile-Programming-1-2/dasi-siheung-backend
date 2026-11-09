package com.dasisiheung.repository;

import com.dasisiheung.entity.member.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 키가 회원 ID 다. findById(memberId) 가 비어 있으면 저장한 적 없는 회원 — 기본값을 쓴다. */
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
}
