package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.space.BuildingUse;
import com.dasisiheung.entity.space.ProofType;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceDetails;
import com.dasisiheung.entity.space.SpaceProof;
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
