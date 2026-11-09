package com.dasisiheung.entity.member;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 방해금지 시간. 시(0~23) 단위이고 시작 시각은 포함, 끝 시각은 포함하지 않는다.
 * <p>
 * 시작이 끝보다 크면 자정을 넘는 구간이다. 예: 22 → 8 이면 22:00 ~ 07:59 동안 푸시를 보내지 않는다.
 * 켜져 있을 때 시작과 끝이 같으면 구간을 정할 수 없으므로 만들 수 없다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DoNotDisturb {

    public static final int MIN_HOUR = 0;
    public static final int MAX_HOUR = 23;

    @Column(name = "dnd_enabled", nullable = false)
    private boolean enabled;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "dnd_start_hour", nullable = false)
    private int startHour;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "dnd_end_hour", nullable = false)
    private int endHour;

    private DoNotDisturb(boolean enabled, int startHour, int endHour) {
        this.enabled = enabled;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    /**
     * 요청 값은 DTO 검증(400 INVALID_INPUT)을 먼저 거치므로, 여기서 실패하면 프로그래밍 오류다.
     *
     * @throws IllegalArgumentException 시각이 0~23 을 벗어나거나, 켜져 있는데 시작과 끝이 같을 때
     */
    public static DoNotDisturb of(boolean enabled, int startHour, int endHour) {
        requireHour(startHour);
        requireHour(endHour);
        if (enabled && startHour == endHour) {
            throw new IllegalArgumentException("방해금지 시작 시각과 끝 시각이 같습니다: " + startHour);
        }
        return new DoNotDisturb(enabled, startHour, endHour);
    }

    /** @param time 서비스 시간대(KST) 기준 시각 */
    public boolean covers(LocalTime time) {
        if (!enabled) {
            return false;
        }
        int hour = time.getHour();
        if (crossesMidnight()) {
            return hour >= startHour || hour < endHour;
        }
        return hour >= startHour && hour < endHour;
    }

    public boolean crossesMidnight() {
        return startHour > endHour;
    }

    private static void requireHour(int hour) {
        if (hour < MIN_HOUR || hour > MAX_HOUR) {
            throw new IllegalArgumentException("시각은 0~23 이어야 합니다: " + hour);
        }
    }
}
