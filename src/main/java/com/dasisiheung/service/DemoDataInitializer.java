package com.dasisiheung.service;

import com.dasisiheung.dto.application.ApplicationRequest;
import com.dasisiheung.dto.application.PriceOfferRequest;
import com.dasisiheung.dto.auth.SignupRequest;
import com.dasisiheung.dto.community.CommentRequest;
import com.dasisiheung.dto.community.PostRequest;
import com.dasisiheung.dto.review.ReviewReplyRequest;
import com.dasisiheung.dto.review.ReviewRequest;
import com.dasisiheung.dto.space.SpaceRequest;
import com.dasisiheung.entity.community.PostCategory;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.BuildingUse;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Facility;
import com.dasisiheung.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 시연 · 테스트 데이터. demo 프로필에서만, 시연 계정이 없을 때 한 번 만든다. (다시 실행해도 중복 생성하지 않음)
 * <p>
 * 실제 서비스 흐름(가입 → 등록 → 신청 → 협의 → 승인 → 후기)을 서비스 메서드로 그대로 밟는다.
 * 관리자 검수와 "지난 이용 기간"만 SQL 로 건너뛴다. 관리자 계정은 만들지 않는다 (ADMIN_GUIDE 의 DB 승격으로 만든다).
 * <p>
 * 배포 서버(prod,demo)에서도 켜 두므로, 만들다 실패해도 서버 기동은 막지 않고 오류만 남긴다.
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    public static final String PASSWORD = "demo1234!";
    static final String MARKER_EMAIL = "provider1@demo.dasisiheung.com";
    /** 사용 가능 날짜: 내일부터 이 기간. 프론트 테스트 기간 동안 날짜가 바닥나지 않게 넉넉히 둔다. */
    static final int CALENDAR_DAYS = 180;

    private final MemberRepository memberRepository;
    private final AuthService authService;
    private final SpaceService spaceService;
    private final ApplicationService applicationService;
    private final ProviderApplicationService providerApplicationService;
    private final PriceNegotiationService priceNegotiationService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;
    private final CommunityService communityService;
    private final ConversationService conversationService;
    private final PrivacyService privacyService;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Override
    public void run(ApplicationArguments args) {
        if (memberRepository.existsByEmail(MARKER_EMAIL)) {
            log.info("시연 데이터가 이미 있어 건너뜁니다.");
            return;
        }
        try {
            createDemoData();
        } catch (RuntimeException e) {
            log.error("시연 데이터를 만들지 못했습니다. 서버는 그대로 실행합니다. "
                    + "*@demo.dasisiheung.com 계정을 지우고 다시 시작하면 처음부터 만듭니다.", e);
        }
    }

    private void createDemoData() {
        LocalDate today = LocalDate.now(clock);
        Set<LocalDate> calendar = today.plusDays(1).datesUntil(today.plusDays(CALENDAR_DAYS + 1)).collect(Collectors.toSet());

        long jeongwangOwner = signup("provider1", "김정왕", Role.PROVIDER);
        long baegotOwner = signup("provider2", "박배곧", Role.PROVIDER);
        long geobukOwner = signup("provider3", "이거북", Role.PROVIDER);
        long popup = signup("user1", "최팝업", Role.USER);
        long artist = signup("user2", "정전시", Role.USER);
        long teacher = signup("user3", "한클래스", Role.USER);
        long maker = signup("user4", "오체험", Role.USER);

        long station = publish(jeongwangOwner, space("정왕역 앞 코너 상가", "역 출구 바로 앞 1층 코너, 퇴근길 유동 인구가 많습니다.",
                "경기도 시흥시 마유로 418", 37.3517, 126.7428, "33.06", BuildingUse.NEIGHBORHOOD_1,
                CommercialTheme.FOOD_ALLEY, 50_000, Set.of(Facility.WIFI, Facility.ELECTRICITY, Facility.WATER, Facility.SIGNBOARD),
                Set.of(ActivityType.POPUP_STORE, ActivityType.EXPERIENCE), calendar));
        long campus = publish(jeongwangOwner, space("한국공학대 앞 쇼룸", "대학가 메인 거리 통유리 쇼룸. 학생 대상 브랜드 홍보에 좋습니다.",
                "경기도 시흥시 산기대학로 237", 37.3401, 126.7335, "45.50", BuildingUse.NEIGHBORHOOD_2,
                CommercialTheme.CAMPUS_TOWN, 60_000, Set.of(Facility.WIFI, Facility.ELECTRICITY, Facility.AIR_CONDITIONER,
                        Facility.TABLE_CHAIR, Facility.DISPLAY_SHELF),
                Set.of(ActivityType.POPUP_STORE, ActivityType.EXHIBITION, ActivityType.MEETING), calendar));
        long cafe = publish(baegotOwner, space("배곧 카페거리 2층 갤러리", "카페거리 2층, 채광 좋은 화이트 톤 공간. 전시 · 클래스용.",
                "경기도 시흥시 배곧4로 32", 37.3773, 126.7302, "52.00", BuildingUse.NEIGHBORHOOD_2,
                CommercialTheme.CAFE_STREET, 70_000, Set.of(Facility.WIFI, Facility.ELECTRICITY, Facility.AIR_CONDITIONER,
                        Facility.HEATING, Facility.TABLE_CHAIR),
                Set.of(ActivityType.EXHIBITION, ActivityType.CLASS), calendar));
        long park = publish(baegotOwner, space("배곧 생명공원 앞 상가", "주말 가족 방문객이 많은 공원 앞. 체험 행사에 알맞습니다.",
                "경기도 시흥시 배곧3로 96", 37.3742, 126.7254, "38.20", BuildingUse.NEIGHBORHOOD_1,
                CommercialTheme.RESIDENTIAL, 45_000, Set.of(Facility.ELECTRICITY, Facility.WATER, Facility.RESTROOM, Facility.SINK),
                Set.of(ActivityType.EXPERIENCE, ActivityType.CLASS, ActivityType.POPUP_STORE), calendar));
        long marina = publish(geobukOwner, space("거북섬 마리나 전망 공간", "바다가 보이는 통유리 1층. 관광객 대상 팝업 · 전시.",
                "경기도 시흥시 거북섬중앙로 2", 37.3175, 126.6822, "60.00", BuildingUse.SALES,
                CommercialTheme.WATERFRONT, 80_000, Set.of(Facility.WIFI, Facility.ELECTRICITY, Facility.AIR_CONDITIONER,
                        Facility.PARKING, Facility.RESTROOM),
                Set.of(ActivityType.POPUP_STORE, ActivityType.EXHIBITION), calendar));
        spaceService.register(geobukOwner, space("거북섬 웨이브파크 옆 창고형 공간", "검수를 기다리는 공간 (관리자 검수 시연용).",
                "경기도 시흥시 거북섬둘레길 42", 37.3152, 126.6791, "120.00", BuildingUse.ETC,
                CommercialTheme.WATERFRONT, 90_000, Set.of(Facility.ELECTRICITY, Facility.PARKING),
                Set.of(ActivityType.POPUP_STORE, ActivityType.MEETING), calendar));

        LocalDate soon = today.plusDays(10);
        // 이용이 끝난 예약 + 후기 (평점 · 통계 시연). 2개는 답글을 비워 제공자 답글 작성을 시험할 수 있게 한다.
        pastBookingWithReview(popup, station, jeongwangOwner, ActivityType.POPUP_STORE, soon, -30, 5,
                "역 앞이라 퇴근 시간에 줄이 생겼어요. 첫날 완판!", true);
        pastBookingWithReview(maker, station, jeongwangOwner, ActivityType.EXPERIENCE, soon, -15, 4,
                "전기 · 수도가 있어 체험 준비가 편했어요.", false);
        pastBookingWithReview(artist, cafe, baegotOwner, ActivityType.EXHIBITION, soon, -20, 5,
                "채광이 좋아 작품 사진이 잘 나왔습니다.", true);
        pastBookingWithReview(teacher, park, baegotOwner, ActivityType.CLASS, soon, -12, 4,
                "가족 단위 참가자가 많았어요. 주차만 조금 아쉬움.", false);
        pastBookingWithReview(popup, marina, geobukOwner, ActivityType.POPUP_STORE, soon, -8, 5,
                "주말 관광객이 정말 많아요. 다음 달에도 신청할게요.", true);

        // 앞으로의 확정 예약 · 협의 중 신청 · 검토 중 신청 (신청 흐름 시연)
        long confirmed = apply(artist, campus, ActivityType.EXHIBITION, "졸업 작품 전시", today.plusDays(14), today.plusDays(16));
        providerApplicationService.approve(jeongwangOwner, confirmed);
        long negotiating = apply(popup, cafe, ActivityType.EXHIBITION, "일러스트 굿즈 전시", today.plusDays(7), today.plusDays(8));
        priceNegotiationService.offer(popup, negotiating, new PriceOfferRequest(120_000, "이틀 연속이라 조금만 조정 부탁드려요."));
        apply(teacher, park, ActivityType.CLASS, "어린이 업사이클링 클래스", today.plusDays(5), today.plusDays(5));
        apply(maker, marina, ActivityType.POPUP_STORE, "수제 캔들 팝업", today.plusDays(20), today.plusDays(21));

        favoriteService.add(popup, marina);
        favoriteService.add(popup, campus);
        favoriteService.add(artist, cafe);
        favoriteService.add(teacher, cafe);
        favoriteService.add(maker, station);

        long recruit = communityService.write(popup, new PostRequest(PostCategory.RECRUIT, "배곧 플리마켓 같이 하실 분 구해요",
                "11월 둘째 주말, 핸드메이드 소품 팝업을 함께 열 팀을 찾습니다. 공간은 배곧 카페거리 쪽으로 알아보고 있어요.")).id();
        communityService.writeComment(maker, recruit, new CommentRequest("캔들 만드는데 같이 하고 싶어요!"));
        communityService.write(artist, new PostRequest(PostCategory.SPACE_STORY, "카페거리 갤러리에서 첫 전시를 열었어요",
                "빈 상가가 이렇게 바뀔 줄 몰랐어요. 다시, 시흥 덕분에 동네 분들께 작품을 보여드렸습니다."));
        communityService.write(baegotOwner, new PostRequest(PostCategory.LOCAL_NEWS, "배곧 생명공원 주말 행사 일정",
                "이번 달 셋째 주 토요일에 공원 가족 축제가 있어요. 근처 공간 이용하실 분들 참고하세요."));
        communityService.write(teacher, new PostRequest(PostCategory.QUESTION, "클래스 준비물 반입 시간 질문",
                "이용 시작일 전날 저녁에 재료를 미리 들여놓을 수 있는지 궁금합니다."));

        long conversation = conversationService.open(maker, marina).id();
        conversationService.send(maker, conversation, "안녕하세요! 주말 이틀 팝업 가능할까요? 테이블 2개 반입하려고 해요.");
        conversationService.send(geobukOwner, conversation, "네 가능합니다. 신청서 보내 주시면 바로 확인할게요!");

        long statistics = privacyService.refreshStatistics();
        log.info("시연 데이터를 만들었습니다. 계정 7개(비밀번호 {}), 공개 공간 5곳 · 검수 대기 1곳, 통계 {}건. "
                + "관리자는 docs/ADMIN_GUIDE.md 대로 승격하세요.", PASSWORD, statistics);
    }

    private long signup(String localPart, String name, Role role) {
        return authService.signup(new SignupRequest(localPart + "@demo.dasisiheung.com", PASSWORD, name,
                "010-0000-0000", role)).id();
    }

    /** 등록하고 관리자 검수를 건너뛰어 바로 공개한다. (시연용) */
    private long publish(long providerId, SpaceRequest request) {
        long spaceId = spaceService.register(providerId, request).id();
        jdbcTemplate.update("UPDATE space SET status = 'PUBLISHED', reviewed_at = now() WHERE id = ?", spaceId);
        return spaceId;
    }

    private long apply(long applicantId, long spaceId, ActivityType activityType, String title, LocalDate start,
                       LocalDate end) {
        return applicationService.submit(applicantId, new ApplicationRequest(spaceId, activityType, title,
                title + " — 시연용 신청입니다.", 2, start, end, null, null)).id();
    }

    /** 앞날짜로 신청 · 승인한 뒤 이용 기간을 과거로 옮기고 후기(와 답글)를 남긴다. */
    private void pastBookingWithReview(long applicantId, long spaceId, long providerId, ActivityType activityType,
                                       LocalDate start, int startOffsetDays, int rating, String content,
                                       boolean replied) {
        long applicationId = apply(applicantId, spaceId, activityType, "시흥 로컬 " + activityType.getLabel(), start,
                start.plusDays(1));
        providerApplicationService.approve(providerId, applicationId);
        jdbcTemplate.update("UPDATE space_application SET start_date = current_date + ?, end_date = current_date + ? "
                + "WHERE id = ?", startOffsetDays, startOffsetDays + 1, applicationId);
        long reviewId = reviewService.write(applicantId, applicationId, new ReviewRequest(rating, content)).id();
        if (replied) {
            reviewService.reply(providerId, reviewId, new ReviewReplyRequest("이용해 주셔서 감사합니다. 또 찾아 주세요!"));
        }
    }

    private static SpaceRequest space(String title, String description, String address, double latitude,
                                      double longitude, String areaSqm, BuildingUse buildingUse,
                                      CommercialTheme theme, int dailyPrice, Set<Facility> facilities,
                                      Set<ActivityType> activityTypes, Set<LocalDate> availableDates) {
        return new SpaceRequest(title, description, "퇴실 시 원상 복구, 21시 이후 소음 금지", address, null, null, latitude,
                longitude, new BigDecimal(areaSqm), buildingUse, theme, dailyPrice, facilities, activityTypes,
                availableDates);
    }
}
