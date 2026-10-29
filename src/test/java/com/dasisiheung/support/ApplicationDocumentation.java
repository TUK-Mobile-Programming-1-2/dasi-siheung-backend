package com.dasisiheung.support;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

import java.util.ArrayList;
import java.util.List;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

/** 신청 API 문서 필드. */
public final class ApplicationDocumentation {

    private ApplicationDocumentation() {
    }

    public static List<FieldDescriptor> requestFields() {
        return List.of(
                fieldWithPath("spaceId").type(JsonFieldType.NUMBER).description("신청할 공간 ID"),
                fieldWithPath("activityType").type(JsonFieldType.STRING).description("활동 종류 (공간이 허용한 활동만)"),
                fieldWithPath("activityTitle").type(JsonFieldType.STRING).description("활동 이름 (최대 60자)"),
                fieldWithPath("plan").type(JsonFieldType.STRING).description("활동 계획 · 공간 사용 방법 (최대 2000자)"),
                fieldWithPath("headcount").type(JsonFieldType.NUMBER).description("예상 운영 인원 (1~200)"),
                fieldWithPath("startDate").type(JsonFieldType.STRING).description("이용 시작일 yyyy-MM-dd (오늘부터)"),
                fieldWithPath("endDate").type(JsonFieldType.STRING).description("이용 종료일 yyyy-MM-dd (최대 30일)"));
    }

    /** 신청 상세 필드. prefix 예: "data." */
    public static List<FieldDescriptor> detailFields(String prefix) {
        return new ArrayList<>(List.of(
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("신청 ID"),
                fieldWithPath(prefix + "status").type(JsonFieldType.STRING)
                        .description("PENDING 검토 중 | APPROVED 예약 확정 | REJECTED 거절 | CANCELLED 취소 | CLOSED 마감(다른 신청 확정)"),
                fieldWithPath(prefix + "statusReason").type(JsonFieldType.STRING).optional().description("거절 · 취소 · 마감 사유"),
                fieldWithPath(prefix + "space.id").type(JsonFieldType.NUMBER).description("공간 ID"),
                fieldWithPath(prefix + "space.title").type(JsonFieldType.STRING).description("공간 이름"),
                fieldWithPath(prefix + "space.region").type(JsonFieldType.STRING).description("서비스 지역"),
                fieldWithPath(prefix + "space.address").type(JsonFieldType.STRING).description("도로명 주소"),
                fieldWithPath(prefix + "space.thumbnailUrl").type(JsonFieldType.STRING).optional().description("대표 사진 주소"),
                fieldWithPath(prefix + "applicant.id").type(JsonFieldType.NUMBER).description("신청자 회원 ID"),
                fieldWithPath(prefix + "applicant.name").type(JsonFieldType.STRING).description("신청자 이름"),
                fieldWithPath(prefix + "activityType").type(JsonFieldType.STRING).description("활동 종류"),
                fieldWithPath(prefix + "activityTitle").type(JsonFieldType.STRING).description("활동 이름"),
                fieldWithPath(prefix + "plan").type(JsonFieldType.STRING).description("활동 계획"),
                fieldWithPath(prefix + "headcount").type(JsonFieldType.NUMBER).description("예상 운영 인원"),
                fieldWithPath(prefix + "startDate").type(JsonFieldType.STRING).description("이용 시작일"),
                fieldWithPath(prefix + "endDate").type(JsonFieldType.STRING).description("이용 종료일"),
                fieldWithPath(prefix + "days").type(JsonFieldType.NUMBER).description("이용 일수"),
                fieldWithPath(prefix + "listDailyPrice").type(JsonFieldType.NUMBER).description("신청 시점 1일 이용료(원)"),
                fieldWithPath(prefix + "listTotalPrice").type(JsonFieldType.NUMBER).description("정가 합계(원)"),
                fieldWithPath(prefix + "totalPrice").type(JsonFieldType.NUMBER).description("현재 합의 금액(원) — 협의 전에는 정가 합계"),
                fieldWithPath(prefix + "negotiationStatus").type(JsonFieldType.STRING)
                        .description("NONE 정가 신청 | WAITING_PROVIDER 제공자 답변 대기 | WAITING_APPLICANT 신청자 답변 대기 | AGREED 가격 합의"),
                fieldWithPath(prefix + "pendingOfferAmount").type(JsonFieldType.NUMBER).optional().description("답변을 기다리는 제안 금액"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("신청 시각"),
                fieldWithPath(prefix + "decidedAt").type(JsonFieldType.STRING).optional().description("확정 · 거절 · 취소 · 마감 시각")));
    }

    /** 신청 목록 항목 필드. prefix 예: "data.content[]." */
    public static FieldDescriptor[] summaryFields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("신청 ID"),
                fieldWithPath(prefix + "status").type(JsonFieldType.STRING).description("신청 상태 (선택지 applicationStatuses)"),
                fieldWithPath(prefix + "spaceId").type(JsonFieldType.NUMBER).description("공간 ID"),
                fieldWithPath(prefix + "spaceTitle").type(JsonFieldType.STRING).description("공간 이름"),
                fieldWithPath(prefix + "thumbnailUrl").type(JsonFieldType.STRING).optional().description("공간 대표 사진 주소"),
                fieldWithPath(prefix + "applicantName").type(JsonFieldType.STRING).description("신청자 이름"),
                fieldWithPath(prefix + "activityType").type(JsonFieldType.STRING).description("활동 종류"),
                fieldWithPath(prefix + "activityTitle").type(JsonFieldType.STRING).description("활동 이름"),
                fieldWithPath(prefix + "startDate").type(JsonFieldType.STRING).description("이용 시작일"),
                fieldWithPath(prefix + "endDate").type(JsonFieldType.STRING).description("이용 종료일"),
                fieldWithPath(prefix + "totalPrice").type(JsonFieldType.NUMBER).description("현재 합의 금액(원)"),
                fieldWithPath(prefix + "negotiationStatus").type(JsonFieldType.STRING).description("가격 협의 상태 (선택지 negotiationStatuses)"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("신청 시각")};
    }
}
