package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static com.dasisiheung.support.ApiDocumentation.validationError;
import static com.dasisiheung.support.MultipartDocumentation.filesPart;
import static com.dasisiheung.support.MultipartDocumentation.requestParts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.inquiry.AdminInquiryResponse;
import com.dasisiheung.dto.inquiry.AnswerInquiryRequest;
import com.dasisiheung.dto.inquiry.InquiryPhotoResponse;
import com.dasisiheung.dto.inquiry.InquiryRequest;
import com.dasisiheung.dto.inquiry.InquiryResponse;
import com.dasisiheung.entity.inquiry.InquiryCategory;
import com.dasisiheung.entity.inquiry.InquiryStatus;
import com.dasisiheung.entity.member.MemberStatus;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.AdminInquiryService;
import com.dasisiheung.service.InquiryService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.TestFiles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

@ApiDocsTest({InquiryController.class, AdminInquiryController.class})
class InquiryControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Inquiry";
    private static final String ADMIN_TAG = "Admin Inquiry";
    private static final long INQUIRY_ID = 12L;
    private static final String SIGNED_URL =
            "https://abc.supabase.co/storage/v1/object/sign/private-files/inquiries/12/photos/a.jpg?token=x";
    private static final String CONTENT = "비밀번호를 바꿨는데 로그인이 안 돼요.";
    private static final String ANSWER = "확인해 보니 예전 비밀번호로 저장된 자동 로그인 정보 때문이었어요. 앱을 다시 실행해 주세요.";
    private static final LocalDateTime ANSWERED_AT = FixedClockConfig.FIXED_LOCAL_NOW.plusHours(2);

    /** 같은 API 의 오류 예시도 같은 설명을 써야 명세의 설명이 예시 문구로 덮이지 않는다. */
    private static final String CREATE_DESCRIPTION = "로그인 회원 누구나 (이용자 · 공간 제공자). JSON 으로 분류 · 내용을 보냅니다. "
            + "내용이 비었거나 1000자를 넘으면 400 INVALID_INPUT. 사진은 응답의 id 로 '1:1 문의 사진 올리기'를 따로 호출하세요.";
    private static final String UPLOAD_DESCRIPTION = "본인 문의에만. multipart/form-data 파트 files(같은 이름으로 여러 개). "
            + "이미 올린 사진과 합쳐 최대 3장 (400 INQUIRY_PHOTO_LIMIT_EXCEEDED), JPG·PNG·WEBP (400 UNSUPPORTED_FILE_TYPE) "
            + "10MB (413 FILE_TOO_LARGE). 답변이 끝난 문의는 409 INQUIRY_ALREADY_ANSWERED, 남의 문의 403 NOT_INQUIRY_OWNER, "
            + "없으면 404 INQUIRY_NOT_FOUND. 사진 하나라도 실패하면 그 요청의 사진은 모두 저장되지 않습니다. "
            + "응답은 이 문의의 전체 사진이며 임시 주소는 10분 뒤 만료됩니다.";
    private static final String DETAIL_DESCRIPTION = "본인 문의만 (남의 문의 403 NOT_INQUIRY_OWNER, 없으면 404 INQUIRY_NOT_FOUND). "
            + "답변이 달리면 status=ANSWERED 와 answer · answeredAt 이 채워집니다. "
            + "사진 임시 주소는 10분 뒤 만료되므로 화면을 열 때마다 조회하세요.";
    private static final String ANSWER_DESCRIPTION = "답변은 한 번만 (이미 답변했으면 409 INQUIRY_ALREADY_ANSWERED). "
            + "답변 최대 2000자. 등록하면 문의한 회원에게 푸시(data.type=INQUIRY_ANSWERED, inquiryId)를 보냅니다.";

    @MockitoBean
    private InquiryService inquiryService;

    @MockitoBean
    private AdminInquiryService adminInquiryService;

    @Test
    @DisplayName("1:1 문의 등록")
    void create() throws Exception {
        given(inquiryService.create(eq(USER_ID), any(InquiryRequest.class)))
                .willReturn(waiting(List.of()));

        mockMvc.perform(post("/api/v1/inquiries").header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InquiryRequest(InquiryCategory.ACCOUNT, CONTENT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.photos").isEmpty())
                .andDo(document("inquiry-create",
                        resource(TAG, "1:1 문의 등록", CREATE_DESCRIPTION),
                        requestHeaders(authorization()),
                        requestFields(
                                fieldWithPath("category").type(JsonFieldType.STRING)
                                        .description("분류 (선택지 inquiryCategories): APPLICATION | PAYMENT | ACCOUNT | SPACE | ETC"),
                                fieldWithPath("content").type(JsonFieldType.STRING).description("문의 내용 (최대 1000자)")),
                        responseFields(success(fields("data.", true)))));

        ArgumentCaptor<InquiryRequest> captor = ArgumentCaptor.forClass(InquiryRequest.class);
        verify(inquiryService).create(eq(USER_ID), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new InquiryRequest(InquiryCategory.ACCOUNT, CONTENT));
    }

    @Test
    @DisplayName("1:1 문의 등록: 공간 제공자도 쓴다")
    void createByProvider() throws Exception {
        given(inquiryService.create(eq(PROVIDER_ID), any(InquiryRequest.class))).willReturn(waiting(List.of()));

        mockMvc.perform(post("/api/v1/inquiries").header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InquiryRequest(InquiryCategory.SPACE, "공간 정보를 고치고 싶어요."))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("1:1 문의 등록: 내용이 비었거나 1000자를 넘으면 400 INVALID_INPUT")
    void createInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/inquiries").header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"ACCOUNT\", \"content\": \"%s\"}".formatted("가".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("content"))
                .andDo(document("inquiry-create-invalid", resource(TAG, "1:1 문의 등록", CREATE_DESCRIPTION),
                        responseFields(validationError())));

        mockMvc.perform(post("/api/v1/inquiries").header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.length()").value(2));
        verifyNoInteractions(inquiryService);
    }

    @Test
    @DisplayName("1:1 문의 등록: 로그인하지 않으면 401")
    void createWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/v1/inquiries").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InquiryRequest(InquiryCategory.ETC, CONTENT))))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(inquiryService);
    }

    @Test
    @DisplayName("1:1 문의 사진 올리기")
    void uploadPhotos() throws Exception {
        given(inquiryService.uploadPhotos(eq(USER_ID), eq(INQUIRY_ID), anyList())).willReturn(waiting(photos(2)));

        mockMvc.perform(multipart("/api/v1/inquiries/{inquiryId}/photos", INQUIRY_ID)
                        .file(TestFiles.jpeg("files"))
                        .file(TestFiles.jpeg("files"))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.photoCount").value(2))
                .andExpect(jsonPath("$.data.photos[0].url").value(SIGNED_URL))
                .andDo(document("inquiry-photo-upload",
                        resource(TAG, "1:1 문의 사진 올리기", UPLOAD_DESCRIPTION),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("inquiryId").description("문의 ID")),
                        requestParts(filesPart("files", "사진 파일 (같은 이름으로 여러 개, 문의당 최대 3장, JPG·PNG·WEBP 10MB)")),
                        responseFields(success(fields("data.", true)))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MultipartFile>> files = ArgumentCaptor.forClass(List.class);
        verify(inquiryService).uploadPhotos(eq(USER_ID), eq(INQUIRY_ID), files.capture());
        assertThat(files.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("1:1 문의 사진 올리기: 3장을 넘으면 400 INQUIRY_PHOTO_LIMIT_EXCEEDED")
    void uploadPhotoLimit() throws Exception {
        given(inquiryService.uploadPhotos(eq(USER_ID), eq(INQUIRY_ID), anyList()))
                .willThrow(new BusinessException(ErrorCode.INQUIRY_PHOTO_LIMIT_EXCEEDED));

        mockMvc.perform(multipart("/api/v1/inquiries/{inquiryId}/photos", INQUIRY_ID)
                        .file(TestFiles.jpeg("files"))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INQUIRY_PHOTO_LIMIT_EXCEEDED"))
                .andDo(document("inquiry-photo-upload-limit", resource(TAG, "1:1 문의 사진 올리기", UPLOAD_DESCRIPTION),
                        responseFields(error())));
    }

    @Test
    @DisplayName("1:1 문의 사진 올리기: 답변이 끝난 문의는 409 INQUIRY_ALREADY_ANSWERED")
    void uploadAfterAnswered() throws Exception {
        given(inquiryService.uploadPhotos(eq(USER_ID), eq(INQUIRY_ID), anyList()))
                .willThrow(new BusinessException(ErrorCode.INQUIRY_ALREADY_ANSWERED));

        mockMvc.perform(multipart("/api/v1/inquiries/{inquiryId}/photos", INQUIRY_ID)
                        .file(TestFiles.jpeg("files"))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INQUIRY_ALREADY_ANSWERED"))
                .andDo(document("inquiry-photo-upload-answered", resource(TAG, "1:1 문의 사진 올리기", UPLOAD_DESCRIPTION),
                        responseFields(error())));
    }

    @Test
    @DisplayName("내 1:1 문의 내역: 최근 순, 사진은 장수만")
    void myInquiries() throws Exception {
        given(inquiryService.getMyInquiries(eq(USER_ID), any(PageQuery.class))).willReturn(new PageResponse<>(List.of(
                new InquiryResponse(13L, InquiryCategory.PAYMENT, "환불은 어떻게 받나요?", InquiryStatus.WAITING, null, null,
                        0, null, FixedClockConfig.FIXED_LOCAL_NOW.plusHours(1)),
                new InquiryResponse(INQUIRY_ID, InquiryCategory.ACCOUNT, CONTENT, InquiryStatus.ANSWERED, ANSWER,
                        ANSWERED_AT, 2, null, FixedClockConfig.FIXED_LOCAL_NOW)),
                0, 20, 2, 1, false));

        mockMvc.perform(get("/api/v1/members/me/inquiries").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("WAITING"))
                .andExpect(jsonPath("$.data.content[1].answer").value(ANSWER))
                .andDo(document("inquiry-my-list",
                        resource(TAG, "내 1:1 문의 내역", "내가 남긴 문의, 최근 순. 답변 대기/완료와 답변을 함께 줍니다. "
                                + "사진은 장수(photoCount)만 주고 photos 는 null 입니다 (사진은 상세에서)."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 20, 최대 100)")),
                        responseFields(page(fields("data.content[].", false)))));
    }

    @Test
    @DisplayName("1:1 문의 상세: 답변 완료 + 사진")
    void detail() throws Exception {
        given(inquiryService.getInquiry(USER_ID, INQUIRY_ID)).willReturn(answered(photos(2)));

        mockMvc.perform(get("/api/v1/inquiries/{inquiryId}", INQUIRY_ID).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ANSWERED"))
                .andExpect(jsonPath("$.data.answeredAt").value("2026-10-20T23:00:00"))
                .andDo(document("inquiry-detail",
                        resource(TAG, "1:1 문의 상세", DETAIL_DESCRIPTION),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("inquiryId").description("문의 ID")),
                        responseFields(success(fields("data.", true)))));
    }

    @Test
    @DisplayName("1:1 문의 상세: 남의 문의는 403 NOT_INQUIRY_OWNER, 없으면 404 INQUIRY_NOT_FOUND")
    void detailErrors() throws Exception {
        given(inquiryService.getInquiry(PROVIDER_ID, INQUIRY_ID))
                .willThrow(new BusinessException(ErrorCode.NOT_INQUIRY_OWNER));
        given(inquiryService.getInquiry(USER_ID, 999L)).willThrow(new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        mockMvc.perform(get("/api/v1/inquiries/{inquiryId}", INQUIRY_ID).header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_INQUIRY_OWNER"))
                .andDo(document("inquiry-detail-forbidden", resource(TAG, "1:1 문의 상세", DETAIL_DESCRIPTION),
                        responseFields(error())));
        mockMvc.perform(get("/api/v1/inquiries/{inquiryId}", 999L).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INQUIRY_NOT_FOUND"))
                .andDo(document("inquiry-detail-not-found", resource(TAG, "1:1 문의 상세", DETAIL_DESCRIPTION),
                        responseFields(error())));
    }

    @Test
    @DisplayName("관리자: 1:1 문의 목록 · 상세 · 답변")
    void adminInquiries() throws Exception {
        given(adminInquiryService.getInquiries(eq(InquiryStatus.WAITING), any(PageQuery.class))).willReturn(
                new PageResponse<>(List.of(adminInquiry(InquiryStatus.WAITING, null)), 0, 20, 1, 1, false));
        given(adminInquiryService.getInquiry(INQUIRY_ID)).willReturn(adminInquiry(InquiryStatus.WAITING, photos(2)));
        given(adminInquiryService.answer(eq(ADMIN_ID), eq(INQUIRY_ID), any(AnswerInquiryRequest.class)))
                .willReturn(adminInquiry(InquiryStatus.ANSWERED, photos(2)));

        mockMvc.perform(get("/api/v1/admin/inquiries").param("status", "WAITING")
                        .header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].photos").isEmpty())
                .andDo(document("admin-inquiry-list",
                        resource(ADMIN_TAG, "1:1 문의 목록", "status=WAITING 이면 오래된 순(먼저 온 문의부터), 그 밖은 최근 순. "
                                + "사진은 장수(photoCount)만."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("status").optional().description("상태 (선택지 inquiryStatuses, 없으면 전체)"),
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(adminFields("data.content[].", false)))));

        mockMvc.perform(get("/api/v1/admin/inquiries/{inquiryId}", INQUIRY_ID).header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andDo(document("admin-inquiry-detail",
                        resource(ADMIN_TAG, "1:1 문의 상세", "사진 임시 주소는 10분 뒤 만료되므로 화면을 열 때마다 조회하세요."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("inquiryId").description("문의 ID")),
                        responseFields(success(adminFields("data.", true)))));

        mockMvc.perform(post("/api/v1/admin/inquiries/{inquiryId}/answer", INQUIRY_ID)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AnswerInquiryRequest(ANSWER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ANSWERED"))
                .andDo(document("admin-inquiry-answer",
                        resource(ADMIN_TAG, "1:1 문의 답변", ANSWER_DESCRIPTION),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("inquiryId").description("문의 ID")),
                        requestFields(fieldWithPath("answer").type(JsonFieldType.STRING).description("답변 내용 (최대 2000자)")),
                        responseFields(success(adminFields("data.", true)))));

        verify(adminInquiryService).getInquiries(eq(InquiryStatus.WAITING), any(PageQuery.class));
    }

    @Test
    @DisplayName("관리자: 상태 없이 전체 목록")
    void adminInquiriesAll() throws Exception {
        given(adminInquiryService.getInquiries(isNull(), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, false));

        mockMvc.perform(get("/api/v1/admin/inquiries").header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("관리자: 이미 답변한 문의는 409 INQUIRY_ALREADY_ANSWERED, 빈 답변은 400")
    void answerErrors() throws Exception {
        given(adminInquiryService.answer(eq(ADMIN_ID), eq(INQUIRY_ID), any(AnswerInquiryRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INQUIRY_ALREADY_ANSWERED));

        mockMvc.perform(post("/api/v1/admin/inquiries/{inquiryId}/answer", INQUIRY_ID)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AnswerInquiryRequest(ANSWER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INQUIRY_ALREADY_ANSWERED"))
                .andDo(document("admin-inquiry-answer-duplicate", resource(ADMIN_TAG, "1:1 문의 답변", ANSWER_DESCRIPTION),
                        responseFields(error())));

        mockMvc.perform(post("/api/v1/admin/inquiries/{inquiryId}/answer", INQUIRY_ID)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("answer"));
    }

    @Test
    @DisplayName("관리자가 아니면 1:1 문의 관리 API 403")
    void adminOnly() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inquiries").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/inquiries/{inquiryId}/answer", INQUIRY_ID)
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AnswerInquiryRequest(ANSWER))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminInquiryService);
    }

    private static InquiryResponse waiting(List<InquiryPhotoResponse> photos) {
        return new InquiryResponse(INQUIRY_ID, InquiryCategory.ACCOUNT, CONTENT, InquiryStatus.WAITING, null, null,
                photos.size(), photos, FixedClockConfig.FIXED_LOCAL_NOW);
    }

    private static InquiryResponse answered(List<InquiryPhotoResponse> photos) {
        return new InquiryResponse(INQUIRY_ID, InquiryCategory.ACCOUNT, CONTENT, InquiryStatus.ANSWERED, ANSWER,
                ANSWERED_AT, photos.size(), photos, FixedClockConfig.FIXED_LOCAL_NOW);
    }

    /** @param photos 상세 · 답변 응답이면 사진 목록, 목록 응답이면 null */
    private static AdminInquiryResponse adminInquiry(InquiryStatus status, List<InquiryPhotoResponse> photos) {
        boolean answered = status == InquiryStatus.ANSWERED;
        return new AdminInquiryResponse(INQUIRY_ID, USER_ID, "이이용", Role.USER, MemberStatus.ACTIVE,
                InquiryCategory.ACCOUNT, CONTENT, status, answered ? ANSWER : null, answered ? ANSWERED_AT : null,
                2, photos, FixedClockConfig.FIXED_LOCAL_NOW);
    }

    private static List<InquiryPhotoResponse> photos(int count) {
        List<InquiryPhotoResponse> photos = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            photos.add(new InquiryPhotoResponse(id, SIGNED_URL));
        }
        return photos;
    }

    /** @param withPhotos 등록 · 사진 올리기 · 상세 응답이면 true (사진 주소 포함), 목록이면 false (photos 는 null) */
    private static FieldDescriptor[] fields(String prefix, boolean withPhotos) {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("문의 ID"),
                fieldWithPath(prefix + "category").type(JsonFieldType.STRING).description("분류 (선택지 inquiryCategories)"),
                fieldWithPath(prefix + "content").type(JsonFieldType.STRING).description("문의 내용"),
                fieldWithPath(prefix + "status").type(JsonFieldType.STRING)
                        .description("상태: WAITING 답변 대기 | ANSWERED 답변 완료"),
                fieldWithPath(prefix + "answer").type(JsonFieldType.STRING).optional()
                        .description("운영자 답변 (답변 전 null)"),
                fieldWithPath(prefix + "answeredAt").type(JsonFieldType.STRING).optional()
                        .description("답변 시각 (답변 전 null)"),
                fieldWithPath(prefix + "photoCount").type(JsonFieldType.NUMBER).description("첨부 사진 장수"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("등록 시각")));
        fields.addAll(photoFields(prefix, withPhotos));
        return fields.toArray(FieldDescriptor[]::new);
    }

    private static FieldDescriptor[] adminFields(String prefix, boolean withPhotos) {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("문의 ID"),
                fieldWithPath(prefix + "memberId").type(JsonFieldType.NUMBER).description("문의한 회원 ID"),
                fieldWithPath(prefix + "memberName").type(JsonFieldType.STRING).description("문의한 회원 이름"),
                fieldWithPath(prefix + "memberRole").type(JsonFieldType.STRING)
                        .description("문의한 회원 역할: USER 이용자 | PROVIDER 공간 제공자"),
                fieldWithPath(prefix + "memberStatus").type(JsonFieldType.STRING)
                        .description("문의한 회원 상태: ACTIVE | SUSPENDED | WITHDRAWN"),
                fieldWithPath(prefix + "category").type(JsonFieldType.STRING).description("분류"),
                fieldWithPath(prefix + "content").type(JsonFieldType.STRING).description("문의 내용"),
                fieldWithPath(prefix + "status").type(JsonFieldType.STRING).description("상태: WAITING | ANSWERED"),
                fieldWithPath(prefix + "answer").type(JsonFieldType.STRING).optional().description("답변 (답변 전 null)"),
                fieldWithPath(prefix + "answeredAt").type(JsonFieldType.STRING).optional()
                        .description("답변 시각 (답변 전 null)"),
                fieldWithPath(prefix + "photoCount").type(JsonFieldType.NUMBER).description("첨부 사진 장수"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("등록 시각")));
        fields.addAll(photoFields(prefix, withPhotos));
        return fields.toArray(FieldDescriptor[]::new);
    }

    private static List<FieldDescriptor> photoFields(String prefix, boolean withPhotos) {
        if (!withPhotos) {
            return List.of(fieldWithPath(prefix + "photos").type(JsonFieldType.NULL).optional()
                    .description("목록에서는 null (사진은 상세에서 확인)"));
        }
        return List.of(
                fieldWithPath(prefix + "photos[]").type(JsonFieldType.ARRAY).description("첨부 사진 (올린 순, 없으면 빈 배열)"),
                fieldWithPath(prefix + "photos[].id").type(JsonFieldType.NUMBER).optional().description("사진 ID"),
                fieldWithPath(prefix + "photos[].url").type(JsonFieldType.STRING).optional()
                        .description("열람용 임시 주소 (전체 URL, 10분 뒤 만료)"));
    }
}
