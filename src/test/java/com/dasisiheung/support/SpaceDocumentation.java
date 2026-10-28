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
                        .description("사용 가능 날짜 yyyy-MM-dd (오늘~1년). 수정 시 보내지 않으면 기존 캘린더 유지"));
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
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("등록 시각"),
                fieldWithPath(prefix + "updatedAt").type(JsonFieldType.STRING).description("마지막 수정 시각")));
        return fields;
    }
}
