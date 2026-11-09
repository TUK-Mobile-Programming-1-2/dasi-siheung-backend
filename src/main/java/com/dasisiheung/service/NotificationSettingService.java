package com.dasisiheung.service;

import com.dasisiheung.dto.member.NotificationSettingRequest;
import com.dasisiheung.dto.member.NotificationSettingResponse;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.NotificationPreferences;
import com.dasisiheung.entity.member.NotificationSetting;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.NotificationSettingRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 알림 설정 조회 · 전체 교체. 저장한 적 없는 회원은 행을 만들지 않고 기본값을 보여준다.
 * <p>
 * 방해금지 · 분류별 수신 여부는 서버가 푸시를 보낼 때 {@link NotificationPreferences#allowsPush} 로 지킨다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    /** @throws BusinessException 없거나 탈퇴한 회원이면 MEMBER_NOT_FOUND */
    public NotificationSettingResponse getMySettings(Long memberId) {
        getMember(memberId);
        return notificationSettingRepository.findById(memberId)
                .map(NotificationSettingResponse::from)
                .orElseGet(NotificationSettingResponse::defaults);
    }

    /**
     * 보낸 값으로 모두 바꾸고 저장된 값을 돌려준다. 처음이면 새로 만든다.
     *
     * @throws BusinessException 없거나 탈퇴한 회원이면 MEMBER_NOT_FOUND
     */
    @Transactional
    public NotificationSettingResponse replaceMySettings(Long memberId, NotificationSettingRequest request) {
        Member member = getMember(memberId);
        NotificationPreferences preferences = request.toPreferences();
        // 동의 시각은 초 단위로 남긴다. (DB 정밀도와 관계없이 저장 직후 응답과 다시 조회한 값이 같다)
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);

        Optional<NotificationSetting> existing = notificationSettingRepository.findById(memberId);
        if (existing.isEmpty()) {
            NotificationSetting created = NotificationSetting.create(member, preferences, now);
            return NotificationSettingResponse.from(notificationSettingRepository.save(created));
        }
        NotificationSetting setting = existing.get();
        setting.replace(preferences, now);
        // 응답의 updatedAt 이 이번 저장 시각이 되도록 커밋 전에 반영한다. (수정 시각은 flush 때 채워진다)
        notificationSettingRepository.flush();
        return NotificationSettingResponse.from(setting);
    }

    /** 탈퇴한 회원은 없는 회원으로 취급한다. (이용 정지 회원은 설정을 볼 수 있다) */
    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .filter(member -> !member.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
