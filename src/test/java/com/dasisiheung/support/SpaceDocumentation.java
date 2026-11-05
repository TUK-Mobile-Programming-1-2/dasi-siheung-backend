package com.dasisiheung.support;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

import java.util.ArrayList;
import java.util.List;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

/** 공간 API 문서의 요청·응답 필드 설명. 여러 테스트가 함께 쓴다. */
public final class SpaceDocumentation {

    private SpaceDocumentation() {
    }

    public static List<FieldDescriptor> spaceRequestFields() {
        return List.of(
                fieldWithPath("title").type(JsonFieldType.STRING).description("공간 이름 (최대 60자)"),
                fieldWithPath("description").type(JsonFieldType.STRING).description("공간 소개 (최대 2000자)"),
                fieldWithPath("rules").type(JsonFieldType.STRING).optional().description("이용 수칙 (선택)"),
                fieldWithPath("address").type(JsonFieldType.STRING).description("도로명 주소"),
                fieldWithPath("buildingName").type(JsonFieldType.STRING).optional().description("건물 이름 (선택)"),
                fieldWithPath("unitNumber").type(JsonFieldType.STRING).optional().description("호실 (선택)"),
                fieldWithPath("latitude").type(JsonFieldType.NUMBER).description("위도 (지도 SDK 로 주소를 변환한 값)"),
                fieldWithPath("longitude").type(JsonFieldType.NUMBER).description("경도"),
                fieldWithPath("areaSqm").type(JsonFieldType.NUMBER).description("전용 면적(㎡), 소수 둘째 자리까지"),
                fieldWithPath("buildingUse").type(JsonFieldType.STRING).description("건축물 용도 (선택지 buildingUses)"),
                fieldWithPath("commercialTheme").type(JsonFieldType.STRING).optional().description("상권 테마 (선택지 commercialThemes, 선택)"),
                fieldWithPath("dailyPrice").type(JsonFieldType.NUMBER).description("1일 이용료(원)"),
                fieldWithPath("facilities").type(JsonFieldType.ARRAY).optional().description("보유 시설 (선택지 facilities)"),
                fieldWithPath("activityTypes").type(JsonFieldType.ARRAY).description("허용 활동 (선택지 activityTypes, 1개 이상)"),
                fieldWithPath("availableDates").type(JsonFieldType.ARRAY).optional()
                        .description("사용 가능 날짜 yyyy-MM-dd (오늘~1년). 수정 시 보내지 않으면 기존 캘린더 유지"),
                fieldWithPath("maxCapacity").type(JsonFieldType.NUMBER).optional()
                        .description("최대 수용 인원 (1~500, 선택). 정하면 이보다 많은 운영 인원의 신청은 400 HEADCOUNT_EXCEEDS_CAPACITY"),
                fieldWithPath("equipment").type(JsonFieldType.ARRAY).optional()
                        .description("빌려주는 비품 (최대 20줄, 선택). 수정할 때는 보낸 목록으로 통째로 바뀝니다"),
                fieldWithPath("equipment[].name").type(JsonFieldType.STRING).optional()
                        .description("비품 이름 (최대 40자, 같은 이름은 한 줄로)"),
                fieldWithPath("equipment[].quantity").type(JsonFieldType.NUMBER).optional().description("수량 (1~999)"),
                fieldWithPath("priceNegotiable").type(JsonFieldType.BOOLEAN).optional()
                        .description("가격 협의 가능 여부 (선택, 기본 true). false 면 가격 제안이 409 PRICE_NEGOTIATION_NOT_ALLOWED"));
    }

    /** 수용 인원 · 비품 · 가격 협의 응답 필드. */
    private static List<FieldDescriptor> capacityFields(String prefix) {
        return List.of(
                fieldWithPath(prefix + "maxCapacity").type(JsonFieldType.NUMBER).optional()
                        .description("최대 수용 인원 (정하지 않았으면 null)"),
                fieldWithPath(prefix + "equipment").type(JsonFieldType.ARRAY).description("빌려주는 비품 (입력 순서대로)"),
                fieldWithPath(prefix + "equipment[].name").type(JsonFieldType.STRING).optional().description("비품 이름"),
                fieldWithPath(prefix + "equipment[].quantity").type(JsonFieldType.NUMBER).optional().description("수량"),
                fieldWithPath(prefix + "priceNegotiable").type(JsonFieldType.BOOLEAN)
                        .description("가격 협의 가능 여부 (false 면 정가로만 신청)"));
    }

    /** 공간 제공자의 내 공간 상세 필드. */
    public static List<FieldDescriptor> providerSpaceFields(String prefix) {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("공간 ID"),
                fieldWithPath(prefix + "status").type(JsonFieldType.STRING)
                        .description("PENDING_REVIEW 검수 대기 | REVISION_REQUESTED 수정 요청 | ON_HOLD 공개 보류 | PUBLISHED 공개 | PAUSED 일시 중단 | HIDDEN 숨김"),
                fieldWithPath(prefix + "reviewComment").type(JsonFieldType.STRING).optional().description("가장 최근 검수 사유"),
                fieldWithPath(prefix + "reviewedAt").type(JsonFieldType.STRING).optional().description("가장 최근 검수 시각"),
                fieldWithPath(prefix + "region").type(JsonFieldType.STRING).description("서비스 지역 (좌표로 판정)"),
                fieldWithPath(prefix + "title").type(JsonFieldType.STRING).description("공간 이름"),
                fieldWithPath(prefix + "description").type(JsonFieldType.STRING).description("공간 소개"),
                fieldWithPath(prefix + "rules").type(JsonFieldType.STRING).optional().description("이용 수칙"),
                fieldWithPath(prefix + "address").type(JsonFieldType.STRING).description("도로명 주소"),
                fieldWithPath(prefix + "buildingName").type(JsonFieldType.STRING).optional().description("건물 이름"),
                fieldWithPath(prefix + "unitNumber").type(JsonFieldType.STRING).optional().description("호실"),
                fieldWithPath(prefix + "latitude").type(JsonFieldType.NUMBER).description("위도"),
                fieldWithPath(prefix + "longitude").type(JsonFieldType.NUMBER).description("경도"),
                fieldWithPath(prefix + "areaSqm").type(JsonFieldType.NUMBER).description("전용 면적(㎡)"),
                fieldWithPath(prefix + "buildingUse").type(JsonFieldType.STRING).description("건축물 용도"),
                fieldWithPath(prefix + "commercialTheme").type(JsonFieldType.STRING).optional().description("상권 테마"),
                fieldWithPath(prefix + "dailyPrice").type(JsonFieldType.NUMBER).description("1일 이용료(원)"),
                fieldWithPath(prefix + "facilities").type(JsonFieldType.ARRAY).description("보유 시설"),
                fieldWithPath(prefix + "activityTypes").type(JsonFieldType.ARRAY).description("허용 활동"),
                fieldWithPath(prefix + "availableDates").type(JsonFieldType.ARRAY).description("사용 가능 날짜 (오름차순)"),
                fieldWithPath(prefix + "photos").type(JsonFieldType.ARRAY).description("사진 (순서대로, 첫 번째가 대표)"),
                fieldWithPath(prefix + "proofs").type(JsonFieldType.ARRAY).description("권한 증빙 (임시 주소 포함)"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("등록 시각"),
                fieldWithPath(prefix + "updatedAt").type(JsonFieldType.STRING).description("마지막 수정 시각")));
        fields.addAll(capacityFields(prefix));
        return fields;
    }

    /** 이용자가 보는 공간 상세 필드 (사진 항목 포함). prefix 예: "data." */
    public static List<FieldDescriptor> spaceDetailFields(String prefix) {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("공간 ID"),
                fieldWithPath(prefix + "region").type(JsonFieldType.STRING).description("서비스 지역"),
                fieldWithPath(prefix + "title").type(JsonFieldType.STRING).description("공간 이름"),
                fieldWithPath(prefix + "description").type(JsonFieldType.STRING).description("공간 소개"),
                fieldWithPath(prefix + "rules").type(JsonFieldType.STRING).optional().description("이용 수칙"),
                fieldWithPath(prefix + "address").type(JsonFieldType.STRING).description("도로명 주소"),
                fieldWithPath(prefix + "buildingName").type(JsonFieldType.STRING).optional().description("건물 이름"),
                fieldWithPath(prefix + "unitNumber").type(JsonFieldType.STRING).optional().description("호실"),
                fieldWithPath(prefix + "latitude").type(JsonFieldType.NUMBER).description("위도"),
                fieldWithPath(prefix + "longitude").type(JsonFieldType.NUMBER).description("경도"),
                fieldWithPath(prefix + "areaSqm").type(JsonFieldType.NUMBER).description("전용 면적(㎡)"),
                fieldWithPath(prefix + "buildingUse").type(JsonFieldType.STRING).description("건축물 용도"),
                fieldWithPath(prefix + "commercialTheme").type(JsonFieldType.STRING).optional().description("상권 테마"),
                fieldWithPath(prefix + "dailyPrice").type(JsonFieldType.NUMBER).description("1일 이용료(원)"),
                fieldWithPath(prefix + "facilities").type(JsonFieldType.ARRAY).description("보유 시설"),
                fieldWithPath(prefix + "activityTypes").type(JsonFieldType.ARRAY).description("허용 활동"),
                fieldWithPath(prefix + "availableDates").type(JsonFieldType.ARRAY).description("오늘 이후 사용 가능 날짜 (오름차순)"),
                fieldWithPath(prefix + "photos").type(JsonFieldType.ARRAY).description("사진 (순서대로, 첫 번째가 대표)"),
                fieldWithPath(prefix + "provider.id").type(JsonFieldType.NUMBER).description("공간 제공자 회원 ID"),
                fieldWithPath(prefix + "provider.name").type(JsonFieldType.STRING).description("공간 제공자 이름"),
                fieldWithPath(prefix + "favoriteCount").type(JsonFieldType.NUMBER).description("찜 수"),
                fieldWithPath(prefix + "reviewCount").type(JsonFieldType.NUMBER).description("후기 수"),
                fieldWithPath(prefix + "ratingAverage").type(JsonFieldType.NUMBER).optional().description("평균 평점 (후기 없으면 null)"),
                fieldWithPath(prefix + "favorited").type(JsonFieldType.BOOLEAN).description("내가 찜했는지 (비로그인이면 false)")));
        fields.addAll(capacityFields(prefix));
        fields.addAll(photoFields(prefix + "photos[]."));
        return fields;
    }

    /** 사진 항목 필드. prefix 예: "data.photos[]." */
    public static List<FieldDescriptor> photoFields(String prefix) {
        return List.of(
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("사진 ID"),
                fieldWithPath(prefix + "url").type(JsonFieldType.STRING).description("사진 주소 (공개, 고정)"),
                fieldWithPath(prefix + "sortOrder").type(JsonFieldType.NUMBER).description("순서 (0 이 대표 사진)"));
    }

    /** 증빙 항목 필드. prefix 예: "data.proofs[]." */
    public static List<FieldDescriptor> proofFields(String prefix) {
        return List.of(
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("증빙 ID"),
                fieldWithPath(prefix + "proofType").type(JsonFieldType.STRING).description("증빙 종류 (선택지 proofTypes)"),
                fieldWithPath(prefix + "originalName").type(JsonFieldType.STRING).optional().description("올린 파일 이름"),
                fieldWithPath(prefix + "contentType").type(JsonFieldType.STRING).description("파일 형식"),
                fieldWithPath(prefix + "fileSize").type(JsonFieldType.NUMBER).description("파일 크기(바이트)"),
                fieldWithPath(prefix + "url").type(JsonFieldType.STRING).description("열람용 임시 주소 (10분 뒤 만료)"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("올린 시각"));
    }
}
