package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.application.ActivityPhotoResponse;
import com.dasisiheung.entity.application.OfferSide;
import com.dasisiheung.entity.application.PhotoPhase;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.ActivityPhotoService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.TestFiles;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockPart;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(ActivityPhotoController.class)
class ActivityPhotoControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Activity Photo";
    private static final String SIGNED_URL =
            "https://abc.supabase.co/storage/v1/object/sign/private-files/applications/7/activity/a.jpg?token=x";

    @MockitoBean
    private ActivityPhotoService activityPhotoService;

    @Test
    @DisplayName("활동 사진 올리기")
    void upload() throws Exception {
        given(activityPhotoService.upload(eq(USER_ID), eq(7L), eq(PhotoPhase.BEFORE), anyList())).willReturn(photos());

        mockMvc.perform(multipart("/api/v1/applications/{applicationId}/activity-photos", 7L)
                        .file(TestFiles.jpeg("files"))
                        .part(new MockPart("phase", "BEFORE".getBytes(StandardCharsets.UTF_8)))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].phase").value("BEFORE"))
                .andDo(document("activity-photo-upload",
                        resource(TAG, "활동 사진 올리기", "확정 예약의 신청자 · 공간 제공자만. 이용 전(BEFORE)은 시작 하루 전~종료일, "
                                + "이용 후(AFTER)는 시작일~종료 7일 뒤 (아니면 409 ACTIVITY_PHOTO_NOT_ALLOWED). "
                                + "시점별 최대 10장 (400 ACTIVITY_PHOTO_LIMIT_EXCEEDED). 응답은 이 예약의 전체 사진입니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("applicationId").description("신청(예약) ID")),
                        requestParts(partWithName("files").description("사진 파일 (여러 개, JPG·PNG·WEBP 10MB)"),
                                partWithName("phase").description("시점 BEFORE 이용 전 | AFTER 이용 후")),
                        responseFields(success(fields()))));
    }

    @Test
    @DisplayName("활동 사진 올리기: 기간 밖이면 409 ACTIVITY_PHOTO_NOT_ALLOWED")
    void notAllowed() throws Exception {
        given(activityPhotoService.upload(eq(USER_ID), eq(7L), eq(PhotoPhase.AFTER), anyList()))
                .willThrow(new BusinessException(ErrorCode.ACTIVITY_PHOTO_NOT_ALLOWED));

        mockMvc.perform(multipart("/api/v1/applications/{applicationId}/activity-photos", 7L)
                        .file(TestFiles.jpeg("files"))
                        .part(new MockPart("phase", "AFTER".getBytes(StandardCharsets.UTF_8)))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isConflict())
                .andDo(document("activity-photo-upload-not-allowed",
                        resource(TAG, "활동 사진 올리기", "이용 시작 전에 이용 후 사진을 올린 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("활동 사진 목록 · 삭제")
    void listAndDelete() throws Exception {
        given(activityPhotoService.getPhotos(PROVIDER_ID, 7L)).willReturn(photos());

        mockMvc.perform(get("/api/v1/applications/{applicationId}/activity-photos", 7L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isOk())
                .andDo(document("activity-photo-list",
                        resource(TAG, "활동 사진 목록", "이용 전 → 이용 후, 올린 순. 임시 주소는 10분 뒤 만료되므로 화면을 열 때마다 조회하세요."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("applicationId").description("신청(예약) ID")),
                        responseFields(success(fields()))));

        mockMvc.perform(delete("/api/v1/applications/{applicationId}/activity-photos/{photoId}", 7L, 1L)
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("activity-photo-delete",
                        resource(TAG, "활동 사진 삭제", "본인이 올린 사진만 (아니면 403 NOT_PHOTO_UPLOADER)."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("applicationId").description("신청(예약) ID"),
                                parameterWithName("photoId").description("사진 ID")),
                        responseFields(fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부 (true)"))));
    }

    private static List<ActivityPhotoResponse> photos() {
        return List.of(
                new ActivityPhotoResponse(1L, PhotoPhase.BEFORE, OfferSide.APPLICANT, "이이용", SIGNED_URL,
                        FixedClockConfig.FIXED_LOCAL_NOW),
                new ActivityPhotoResponse(2L, PhotoPhase.AFTER, OfferSide.PROVIDER, "김제공", SIGNED_URL,
                        FixedClockConfig.FIXED_LOCAL_NOW));
    }

    private static FieldDescriptor[] fields() {
        return new FieldDescriptor[]{
                fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("사진 ID"),
                fieldWithPath("data[].phase").type(JsonFieldType.STRING).description("BEFORE 이용 전 | AFTER 이용 후"),
                fieldWithPath("data[].uploaderSide").type(JsonFieldType.STRING).description("APPLICANT 신청자 | PROVIDER 공간 제공자"),
                fieldWithPath("data[].uploaderName").type(JsonFieldType.STRING).description("올린 회원 이름"),
                fieldWithPath("data[].url").type(JsonFieldType.STRING).description("열람용 임시 주소 (10분 뒤 만료)"),
                fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("올린 시각")};
    }
}
