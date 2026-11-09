package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.member.DoNotDisturb;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.NotificationCategory;
import com.dasisiheung.entity.member.NotificationPreferences;
import com.dasisiheung.entity.member.NotificationSetting;
import com.dasisiheung.entity.member.Role;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class NotificationSettingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 21, 0);
    private static final LocalTime NOON = LocalTime.of(12, 0);

    @Nested
    @DisplayName("방해금지 시간")
    class DoNotDisturbTest {

        @ParameterizedTest(name = "22시 → 8시, {0} → {1}")
        @CsvSource({
                "21:59, false",
                "22:00, true",
                "23:59, true",
                "00:00, true",
                "07:59, true",
                "08:00, false",
                "12:00, false"
        })
        @DisplayName("시작이 끝보다 크면 자정을 넘는다: 시작은 포함, 끝은 포함하지 않는다")
        void crossesMidnight(LocalTime time, boolean expected) {
            DoNotDisturb dnd = DoNotDisturb.of(true, 22, 8);

            assertThat(dnd.crossesMidnight()).isTrue();
            assertThat(dnd.covers(time)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "13시 → 15시, {0} → {1}")
        @CsvSource({
                "12:59, false",
                "13:00, true",
                "14:59, true",
                "15:00, false"
        })
        @DisplayName("시작이 끝보다 작으면 같은 날 안의 구간이다")
        void sameDay(LocalTime time, boolean expected) {
            DoNotDisturb dnd = DoNotDisturb.of(true, 13, 15);

            assertThat(dnd.crossesMidnight()).isFalse();
            assertThat(dnd.covers(time)).isEqualTo(expected);
        }

        @Test
        @DisplayName("0시 → 23시는 23시 한 시간만 빼고 모두 방해금지다")
        void almostWholeDay() {
            DoNotDisturb dnd = DoNotDisturb.of(true, 0, 23);

            assertThat(dnd.covers(LocalTime.of(0, 0))).isTrue();
            assertThat(dnd.covers(LocalTime.of(22, 59))).isTrue();
            assertThat(dnd.covers(LocalTime.of(23, 0))).isFalse();
        }

        @Test
        @DisplayName("꺼져 있으면 어느 시각도 방해금지가 아니다")
        void disabled() {
            DoNotDisturb dnd = DoNotDisturb.of(false, 22, 8);

            assertThat(dnd.covers(LocalTime.of(23, 0))).isFalse();
            assertThat(dnd.covers(LocalTime.of(3, 0))).isFalse();
        }

        @Test
        @DisplayName("꺼져 있으면 시작과 끝이 같아도 보관할 수 있다")
        void disabledWithSameHours() {
            DoNotDisturb dnd = DoNotDisturb.of(false, 9, 9);

            assertThat(dnd.getStartHour()).isEqualTo(9);
            assertThat(dnd.covers(LocalTime.of(9, 0))).isFalse();
        }

        @Test
        @DisplayName("켜져 있는데 시작과 끝이 같으면 만들 수 없다")
        void enabledWithSameHours() {
            assertThatThrownBy(() -> DoNotDisturb.of(true, 9, 9))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 24})
        @DisplayName("0~23 을 벗어난 시각은 만들 수 없다")
        void outOfRange(int hour) {
            assertThatThrownBy(() -> DoNotDisturb.of(false, hour, 8)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> DoNotDisturb.of(false, 22, hour)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("푸시 허용 여부")
    class AllowsPushTest {

        @ParameterizedTest
        @EnumSource(NotificationCategory.class)
        @DisplayName("푸시 전체를 끄면 어떤 분류도 보내지 않는다")
        void pushOff(NotificationCategory category) {
            NotificationPreferences preferences = new NotificationPreferences(false, true, true, true, true,
                    DoNotDisturb.of(false, 22, 8));

            assertThat(preferences.allowsPush(category, NOON)).isFalse();
        }

        @Test
        @DisplayName("분류별 설정을 따른다")
        void byCategory() {
            NotificationPreferences preferences = new NotificationPreferences(true, true, false, true, false,
                    DoNotDisturb.of(false, 22, 8));

            assertThat(preferences.allowsPush(NotificationCategory.CHAT, NOON)).isTrue();
            assertThat(preferences.allowsPush(NotificationCategory.ACTIVITY, NOON)).isFalse();
            assertThat(preferences.allowsPush(NotificationCategory.FAVORITE_SPACE, NOON)).isTrue();
            assertThat(preferences.allowsPush(NotificationCategory.MARKETING, NOON)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(NotificationCategory.class)
        @DisplayName("방해금지 시간에는 켜진 분류도 보내지 않는다")
        void duringDoNotDisturb(NotificationCategory category) {
            NotificationPreferences preferences = new NotificationPreferences(true, true, true, true, true,
                    DoNotDisturb.of(true, 22, 8));

            assertThat(preferences.allowsPush(category, LocalTime.of(23, 30))).isFalse();
            assertThat(preferences.allowsPush(category, NOON)).isTrue();
        }

        @Test
        @DisplayName("기본값: 푸시 · 대화 · 활동은 켜짐, 찜 · 광고는 꺼짐, 방해금지 22시 → 8시")
        void defaults() {
            NotificationPreferences preferences = NotificationPreferences.DEFAULT;

            assertThat(preferences.push()).isTrue();
            assertThat(preferences.chat()).isTrue();
            assertThat(preferences.activity()).isTrue();
            assertThat(preferences.favoriteSpace()).isFalse();
            assertThat(preferences.marketing()).isFalse();
            assertThat(preferences.doNotDisturb()).isEqualTo(DoNotDisturb.of(true, 22, 8));
        }
    }

    @Nested
    @DisplayName("광고성 정보 수신 동의 시각")
    class MarketingConsentTest {

        private final Member member = Member.signUp("user@example.com", "hash", "김시흥", null, Role.USER);

        @Test
        @DisplayName("처음 저장할 때 동의하면 그 시각을 남긴다")
        void consentOnCreate() {
            NotificationSetting setting = NotificationSetting.create(member, marketing(true), NOW);

            assertThat(setting.isMarketingEnabled()).isTrue();
            assertThat(setting.getMarketingConsentedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("처음 저장할 때 동의하지 않으면 시각이 없다")
        void noConsentOnCreate() {
            NotificationSetting setting = NotificationSetting.create(member, marketing(false), NOW);

            assertThat(setting.getMarketingConsentedAt()).isNull();
        }

        @Test
        @DisplayName("동의 중에 다시 저장해도 처음 동의한 시각을 유지한다")
        void keepOriginalConsentTime() {
            NotificationSetting setting = NotificationSetting.create(member, marketing(true), NOW);

            setting.replace(marketing(true), NOW.plusDays(3));

            assertThat(setting.getMarketingConsentedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("철회하면 시각을 지우고, 다시 동의하면 새 시각을 남긴다")
        void withdrawThenConsentAgain() {
            NotificationSetting setting = NotificationSetting.create(member, marketing(true), NOW);

            setting.replace(marketing(false), NOW.plusDays(1));
            assertThat(setting.isMarketingEnabled()).isFalse();
            assertThat(setting.getMarketingConsentedAt()).isNull();

            setting.replace(marketing(true), NOW.plusDays(2));
            assertThat(setting.getMarketingConsentedAt()).isEqualTo(NOW.plusDays(2));
        }

        @Test
        @DisplayName("전체 교체: 저장한 값이 그대로 돌아온다")
        void replaceAll() {
            NotificationSetting setting = NotificationSetting.create(member, NotificationPreferences.DEFAULT, NOW);
            NotificationPreferences changed = new NotificationPreferences(false, false, true, true, true,
                    DoNotDisturb.of(false, 1, 6));

            setting.replace(changed, NOW);

            assertThat(setting.preferences()).isEqualTo(changed);
        }

        private static NotificationPreferences marketing(boolean consents) {
            return new NotificationPreferences(true, true, true, false, consents, DoNotDisturb.of(true, 22, 8));
        }
    }
}
