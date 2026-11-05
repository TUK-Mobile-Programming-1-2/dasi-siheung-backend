package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.space.BuildingUse;
import com.dasisiheung.entity.space.Equipment;
import com.dasisiheung.entity.space.ProofType;
import com.dasisiheung.entity.space.ReviewChecklist;
import com.dasisiheung.entity.space.ReviewDecision;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceDetails;
import com.dasisiheung.entity.space.SpaceProof;
import com.dasisiheung.entity.space.SpaceReview;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.support.SpaceFixtures;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SpaceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    private static final LocalDateTime NOW = TODAY.atTime(21, 0);

    @Test
    @DisplayName("등록하면 검수 대기 상태이고, 빈 선택 항목은 null 로 정리된다")
    void register() {
        var request = SpaceFixtures.request(Set.of(TODAY));
        SpaceDetails details = new SpaceDetails(request.title(), request.description(), " ", request.address(), "",
                null, request.toDetails().location(), request.areaSqm(), request.buildingUse(), null,
                request.dailyPrice(), request.facilities(), request.activityTypes());

        Space space = SpaceFixtures.space(1L, SpaceFixtures.provider(2L), Set.of(TODAY), TODAY);
        Space blankFields = Space.register(SpaceFixtures.provider(2L), space.getRegion(), details, null, TODAY);

        assertThat(space.getStatus()).isEqualTo(SpaceStatus.PENDING_REVIEW);
        assertThat(space.isOwnedBy(2L)).isTrue();
        assertThat(space.isOwnedBy(3L)).isFalse();
        assertThat(blankFields.getRules()).isNull();
        assertThat(blankFields.getBuildingName()).isNull();
        assertThat(blankFields.getAvailableDates()).isEmpty();
    }

    @Test
    @DisplayName("사용 가능 날짜는 오늘부터 365일 뒤까지만 허용한다")
    void availableDateWindow() {
        Space space = SpaceFixtures.space(1L, SpaceFixtures.provider(2L), Set.of(), TODAY);

        space.changeAvailableDates(Set.of(TODAY, TODAY.plusDays(365)), TODAY);
        assertThat(space.getAvailableDates()).containsExactlyInAnyOrder(TODAY, TODAY.plusDays(365));

        assertThatThrownBy(() -> space.changeAvailableDates(Set.of(TODAY.minusDays(1)), TODAY))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AVAILABLE_DATE);
        assertThatThrownBy(() -> space.changeAvailableDates(Set.of(TODAY.plusDays(366)), TODAY))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AVAILABLE_DATE);
    }

    @Test
    @DisplayName("사진은 최대 10장이고, 순서를 바꾸면 첫 번째가 대표가 된다")
    void photos() {
        Space space = SpaceFixtures.space(1L, SpaceFixtures.provider(2L), Set.of(), TODAY);
        for (long id = 1; id <= Space.MAX_PHOTOS; id++) {
            ReflectionTestUtils.setField(space.addPhoto("p" + id + ".jpg", NOW), "id", id);
        }

        assertThatThrownBy(() -> space.addPhoto("over.jpg", NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_PHOTO_LIMIT_EXCEEDED);
        assertThatThrownBy(() -> space.validateCanAddPhotos(1))
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_PHOTO_LIMIT_EXCEEDED);

        List<Long> reversed = LongStream.rangeClosed(1, Space.MAX_PHOTOS).boxed()
                .sorted(Comparator.reverseOrder()).toList();
        space.reorderPhotos(reversed);
        assertThat(space.getPhotos().get(0).getId()).isEqualTo(Space.MAX_PHOTOS);
        assertThat(space.getPhotos().get(0).getSortOrder()).isZero();

        assertThatThrownBy(() -> space.reorderPhotos(List.of(1L, 2L)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PHOTO_ORDER);
        assertThat(space.removePhoto(3L).getStoragePath()).isEqualTo("p3.jpg");
        assertThatThrownBy(() -> space.removePhoto(3L))
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_PHOTO_NOT_FOUND);
    }

    @Test
    @DisplayName("공개 중인 공간의 증빙을 바꾸면 다시 검수 대기가 된다")
    void proofChangeRequestsReReview() {
        Space space = SpaceFixtures.space(1L, SpaceFixtures.provider(2L), Set.of(), TODAY);
        ReflectionTestUtils.setField(space, "status", SpaceStatus.PUBLISHED);

        SpaceProof proof = space.addProof(ProofType.LEASE_CONTRACT,
                new SpaceProof.StoredProofFile("proofs/a.pdf", "계약서.pdf", "application/pdf", 10), NOW);

        assertThat(space.getStatus()).isEqualTo(SpaceStatus.PENDING_REVIEW);
        assertThat(proof.getProofType()).isEqualTo(ProofType.LEASE_CONTRACT);
    }

    @Test
    @DisplayName("관리자가 숨긴 공간은 수정할 수 없고, 다른 제공자는 관리할 수 없다")
    void hiddenAndOwnership() {
        Space space = SpaceFixtures.space(1L, SpaceFixtures.provider(2L), Set.of(), TODAY);

        assertThatThrownBy(() -> space.validateOwner(3L))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_SPACE_OWNER);

        ReflectionTestUtils.setField(space, "status", SpaceStatus.HIDDEN);
        assertThatThrownBy(() -> space.addPhoto("a.jpg", NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_NOT_EDITABLE);
    }

    @Test
    @DisplayName("공개 중에 용도를 바꾸면 다시 검수 대기, 가격만 바꾸면 공개 유지")
    void updateReReviewRule() {
        Space space = published();
        SpaceDetails base = SpaceFixtures.request(Set.of()).toDetails();
        SpaceDetails priceOnly = new SpaceDetails(base.title(), base.description(), base.rules(), base.address(),
                base.buildingName(), base.unitNumber(), base.location(), base.areaSqm(), base.buildingUse(),
                base.commercialTheme(), 80_000, base.facilities(), base.activityTypes());
        SpaceDetails useChanged = new SpaceDetails(base.title(), base.description(), base.rules(), base.address(),
                base.buildingName(), base.unitNumber(), base.location(), base.areaSqm(), BuildingUse.SALES,
                base.commercialTheme(), base.dailyPrice(), base.facilities(), base.activityTypes());

        space.update(space.getRegion(), priceOnly, null, TODAY);
        assertThat(space.getStatus()).isEqualTo(SpaceStatus.PUBLISHED);
        assertThat(space.getDailyPrice()).isEqualTo(80_000);

        space.update(space.getRegion(), useChanged, Set.of(TODAY.plusDays(3)), TODAY);
        assertThat(space.getStatus()).isEqualTo(SpaceStatus.PENDING_REVIEW);
        assertThat(space.getAvailableDates()).containsExactly(TODAY.plusDays(3));
    }

    @Test
    @DisplayName("일시 중단은 공개 중에만, 재개는 일시 중단 중에만, 재검수 요청은 수정 요청·보류 중에만")
    void statusTransitions() {
        Space space = published();

        space.pause();
        assertThat(space.getStatus()).isEqualTo(SpaceStatus.PAUSED);
        assertThatThrownBy(space::pause).extracting("errorCode").isEqualTo(ErrorCode.INVALID_SPACE_STATUS);

        space.resume();
        assertThat(space.getStatus()).isEqualTo(SpaceStatus.PUBLISHED);
        assertThatThrownBy(space::resubmit).extracting("errorCode").isEqualTo(ErrorCode.INVALID_SPACE_STATUS);

        ReflectionTestUtils.setField(space, "status", SpaceStatus.REVISION_REQUESTED);
        space.resubmit();
        assertThat(space.getStatus()).isEqualTo(SpaceStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("공개 승인은 증빙 1개 이상 + 5개 항목 모두 확인, 수정 요청·보류는 사유 필수, 검수 대기에서만")
    void reviewRules() {
        Space space = SpaceFixtures.space(1L, SpaceFixtures.provider(2L), Set.of(), TODAY);
        Member admin = SpaceFixtures.provider(9L);
        ReviewChecklist allPassed = new ReviewChecklist(true, true, true, true, true);
        ReviewChecklist areaMismatch = new ReviewChecklist(true, true, true, false, true);

        assertThatThrownBy(() -> space.review(admin, ReviewDecision.APPROVE, null, allPassed, NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_PROOF_REQUIRED);

        space.addProof(ProofType.LEASE_CONTRACT,
                new SpaceProof.StoredProofFile("proofs/a.pdf", "a.pdf", "application/pdf", 1), NOW);
        assertThatThrownBy(() -> space.review(admin, ReviewDecision.APPROVE, null, areaMismatch, NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_CHECKLIST_INCOMPLETE);
        assertThatThrownBy(() -> space.review(admin, ReviewDecision.REQUEST_REVISION, " ", areaMismatch, NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_REASON_REQUIRED);

        SpaceReview revision = space.review(admin, ReviewDecision.REQUEST_REVISION, "면적이 증빙과 다릅니다.", areaMismatch, NOW);
        assertThat(space.getStatus()).isEqualTo(SpaceStatus.REVISION_REQUESTED);
        assertThat(space.getReviewComment()).isEqualTo("면적이 증빙과 다릅니다.");
        assertThat(revision.getChecklist().isAreaMatched()).isFalse();
        assertThatThrownBy(() -> space.review(admin, ReviewDecision.APPROVE, null, allPassed, NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SPACE_STATUS);

        space.resubmit();
        space.review(admin, ReviewDecision.APPROVE, null, allPassed, NOW);
        assertThat(space.getStatus()).isEqualTo(SpaceStatus.PUBLISHED);
        assertThat(space.getReviewComment()).isNull();
        assertThat(space.getReviewedAt()).isEqualTo(NOW);
    }

    private static Space published() {
        Space space = SpaceFixtures.space(1L, SpaceFixtures.provider(2L), Set.of(), TODAY);
        ReflectionTestUtils.setField(space, "status", SpaceStatus.PUBLISHED);
        return space;
    }

    @Test
    @DisplayName("수용 인원 · 비품 · 가격 협의는 바꿔도 다시 검수받지 않고, 비품은 보낸 목록으로 통째로 바뀐다")
    void capacityEquipmentNegotiation() {
        Space space = published();
        assertThat(space.getMaxCapacity()).isEqualTo(10);
        assertThat(space.getEquipment()).extracting(Equipment::getName).containsExactly("접이식 테이블", "의자");
        assertThat(space.isPriceNegotiable()).isTrue();

        SpaceDetails base = SpaceFixtures.request(Set.of()).toDetails();
        space.update(space.getRegion(), new SpaceDetails(base.title(), base.description(), base.rules(),
                base.address(), base.buildingName(), base.unitNumber(), base.location(), base.areaSqm(),
                base.buildingUse(), base.commercialTheme(), base.dailyPrice(), base.facilities(), base.activityTypes(),
                30, List.of(new Equipment(" 빔프로젝터 ", 1)), false), null, TODAY);

        assertThat(space.getStatus()).isEqualTo(SpaceStatus.PUBLISHED);
        assertThat(space.getMaxCapacity()).isEqualTo(30);
        assertThat(space.isPriceNegotiable()).isFalse();
        assertThat(space.getEquipment()).containsExactly(new Equipment("빔프로젝터", 1));
        assertThat(space.canAccommodate(30)).isTrue();
        assertThat(space.canAccommodate(31)).isFalse();
    }

    @Test
    @DisplayName("수용 인원을 정하지 않은 공간은 인원을 제한하지 않는다 (기존 등록 항목만 있는 공간)")
    void noCapacity() {
        SpaceDetails base = SpaceFixtures.request(Set.of()).toDetails();
        Space space = Space.register(SpaceFixtures.provider(2L), published().getRegion(),
                new SpaceDetails(base.title(), base.description(), base.rules(), base.address(), base.buildingName(),
                        base.unitNumber(), base.location(), base.areaSqm(), base.buildingUse(), base.commercialTheme(),
                        base.dailyPrice(), base.facilities(), base.activityTypes()), Set.of(), TODAY);

        assertThat(space.getMaxCapacity()).isNull();
        assertThat(space.canAccommodate(500)).isTrue();
        assertThat(space.getEquipment()).isEmpty();
        assertThat(space.isPriceNegotiable()).isTrue();
    }

    @Test
    @DisplayName("주소·건물·호실·면적·용도가 바뀌면 검수 항목이 바뀐 것으로 본다 (이름·가격은 아님)")
    void reviewedFieldChanges() {
        SpaceDetails base = SpaceFixtures.request(Set.of()).toDetails();
        SpaceDetails samePriceChanged = new SpaceDetails("새 이름", base.description(), base.rules(), base.address(),
                base.buildingName(), base.unitNumber(), base.location(), new BigDecimal("33.060"), base.buildingUse(),
                base.commercialTheme(), 70_000, base.facilities(), base.activityTypes());
        SpaceDetails useChanged = new SpaceDetails(base.title(), base.description(), base.rules(), base.address(),
                base.buildingName(), base.unitNumber(), base.location(), base.areaSqm(), BuildingUse.SALES,
                base.commercialTheme(), base.dailyPrice(), base.facilities(), base.activityTypes());
        SpaceDetails unitChanged = new SpaceDetails(base.title(), base.description(), base.rules(), base.address(),
                base.buildingName(), "102호", base.location(), base.areaSqm(), base.buildingUse(),
                base.commercialTheme(), base.dailyPrice(), base.facilities(), base.activityTypes());

        assertThat(base.differsInReviewedFields(samePriceChanged)).isFalse();
        assertThat(base.differsInReviewedFields(useChanged)).isTrue();
        assertThat(base.differsInReviewedFields(unitChanged)).isTrue();
    }
}
