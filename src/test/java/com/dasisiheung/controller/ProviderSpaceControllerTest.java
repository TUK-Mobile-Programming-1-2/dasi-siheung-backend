package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.space.ProviderSpaceResponse;
import com.dasisiheung.dto.space.ProviderSpaceSummaryResponse;
import com.dasisiheung.dto.space.SpacePhotoResponse;
import com.dasisiheung.dto.space.SpaceProofResponse;
import com.dasisiheung.dto.space.SpaceRequest;
import com.dasisiheung.entity.space.ProofType;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.SpaceFileService;
import com.dasisiheung.service.SpaceService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceDocumentation;
import com.dasisiheung.support.SpaceFixtures;
import com.dasisiheung.support.TestFiles;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(ProviderSpaceController.class)
class ProviderSpaceControllerTest extends ApiDocsTestSupport {

    static final String TAG = "Provider Space";
    static final LocalDate TOMORROW = FixedClockConfig.FIXED_TODAY.plusDays(1);

    @MockitoBean
    private SpaceService spaceService;

    @MockitoBean
    private SpaceFileService spaceFileService;

    @Test
    @DisplayName("공간 등록: 201 과 검수 대기 상태의 내 공간 상세를 반환한다")
    void register() throws Exception {
        SpaceRequest request = SpaceFixtures.request(Set.of(TOMORROW, TOMORROW.plusDays(1)));
        given(spaceService.register(eq(PROVIDER_ID), any(SpaceRequest.class))).willReturn(providerSpace());

        mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.region").value("JEONGWANG"))
                .andDo(document("provider-space-register",
                        resource(TAG, "공간 등록", "지역은 좌표로 판정하며 정왕동·거북섬·배곧 밖이면 400 SPACE_OUT_OF_SERVICE_AREA 입니다. "
                                + "등록하면 검수 대기(PENDING_REVIEW) 상태가 되고, 사진·증빙은 등록 후 따로 올립니다."),
                        requestHeaders(authorization()),
                        requestFields(SpaceDocumentation.spaceRequestFields()),
                        responseFields(success(providerSpaceFields()))));
    }

    @Test
    @DisplayName("공간 등록: 서비스 지역 밖이면 400 SPACE_OUT_OF_SERVICE_AREA")
    void registerOutOfServiceArea() throws Exception {
        given(spaceService.register(eq(PROVIDER_ID), any(SpaceRequest.class)))
                .willThrow(new BusinessException(ErrorCode.SPACE_OUT_OF_SERVICE_AREA));

        mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(SpaceFixtures.request(SpaceFixtures.SEOUL_LAT, SpaceFixtures.SEOUL_LNG, Set.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SPACE_OUT_OF_SERVICE_AREA"))
                .andDo(document("provider-space-register-out-of-area",
                        resource(TAG, "공간 등록", "서비스 지역 밖 좌표 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("공간 등록: 이용자(USER)는 403 FORBIDDEN")
    void registerByUserIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(SpaceFixtures.request(Set.of()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("공간 등록: 허용 활동이 비어 있으면 400")
    void registerWithoutActivities() throws Exception {
        mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "상가", "description": "소개", "address": "경기도 시흥시 마유로 418",
                                 "latitude": 37.3517, "longitude": 126.7428, "areaSqm": 33.06,
                                 "buildingUse": "NEIGHBORHOOD_1", "dailyPrice": 50000, "activityTypes": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("activityTypes"));
    }

    @Test
    @DisplayName("사진 추가: 여러 장을 올리면 전체 사진 목록을 순서대로 반환한다")
    void addPhotos() throws Exception {
        given(spaceFileService.addPhotos(eq(PROVIDER_ID), eq(10L), anyList())).willReturn(photos());

        mockMvc.perform(multipart("/api/v1/provider/spaces/{spaceId}/photos", 10L)
                        .file(TestFiles.jpeg("files"))
                        .file(TestFiles.jpeg("files"))
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andDo(document("provider-space-photo-add",
                        resource(TAG, "사진 추가", "multipart 파트 files 에 여러 장을 담아 보냅니다. 공간당 최대 10장, "
                                + "JPG·PNG·WEBP 10MB. 초과하면 400 SPACE_PHOTO_LIMIT_EXCEEDED, 형식이 다르면 400 UNSUPPORTED_FILE_TYPE."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        requestParts(partWithName("files").description("사진 파일 (여러 개)")),
                        responseFields(success(photoListFields()))));
    }

    @Test
    @DisplayName("사진 순서 변경")
    void reorderPhotos() throws Exception {
        given(spaceFileService.reorderPhotos(PROVIDER_ID, 10L, List.of(2L, 1L))).willReturn(photos());

        mockMvc.perform(put("/api/v1/provider/spaces/{spaceId}/photos/order", 10L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"photoIds": [2, 1]}
                                """))
                .andExpect(status().isOk())
                .andDo(document("provider-space-photo-order",
                        resource(TAG, "사진 순서 변경", "이 공간의 모든 사진 ID 를 원하는 순서대로 한 번씩 보냅니다. "
                                + "빠지거나 다른 공간의 ID 가 있으면 400 INVALID_PHOTO_ORDER."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        requestFields(fieldWithPath("photoIds").type(JsonFieldType.ARRAY).description("사진 ID 목록 (첫 번째가 대표)")),
                        responseFields(success(photoListFields()))));
    }

    @Test
    @DisplayName("사진 삭제")
    void deletePhoto() throws Exception {
        mockMvc.perform(delete("/api/v1/provider/spaces/{spaceId}/photos/{photoId}", 10L, 1L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isOk())
                .andDo(document("provider-space-photo-delete",
                        resource(TAG, "사진 삭제", "저장소 파일은 삭제가 확정된 뒤 지워집니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID"),
                                parameterWithName("photoId").description("사진 ID")),
                        responseFields(success())));
        then(spaceFileService).should().deletePhoto(PROVIDER_ID, 10L, 1L);
    }

    @Test
    @DisplayName("다른 제공자의 공간이면 403 NOT_SPACE_OWNER")
    void deletePhotoOfOthers() throws Exception {
        willThrow(new BusinessException(ErrorCode.NOT_SPACE_OWNER))
                .given(spaceFileService).deletePhoto(PROVIDER_ID, 99L, 1L);

        mockMvc.perform(delete("/api/v1/provider/spaces/{spaceId}/photos/{photoId}", 99L, 1L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_SPACE_OWNER"));
    }

    @Test
    @DisplayName("권한 증빙 추가: 비공개 저장소에 올리고 임시 주소를 반환한다")
    void addProof() throws Exception {
        given(spaceFileService.addProof(eq(PROVIDER_ID), eq(10L), eq(ProofType.LEASE_CONTRACT), any()))
                .willReturn(proof());

        mockMvc.perform(multipart("/api/v1/provider/spaces/{spaceId}/proofs", 10L)
                        .file(TestFiles.pdf("file"))
                        .part(new MockPart("proofType", "LEASE_CONTRACT".getBytes(StandardCharsets.UTF_8)))
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.proofType").value("LEASE_CONTRACT"))
                .andDo(document("provider-space-proof-add",
                        resource(TAG, "권한 증빙 추가", "PDF·JPG·PNG 20MB, 공간당 최대 5개. "
                                + "공개·일시 중단 중인 공간에 추가하면 다시 검수 대기(PENDING_REVIEW)가 됩니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        requestParts(partWithName("file").description("증빙 파일"),
                                partWithName("proofType").description("증빙 종류 (선택지 proofTypes)")),
                        responseFields(success(proofFields("data.")))));
    }

    @Test
    @DisplayName("권한 증빙 목록: 조회할 때마다 새 임시 주소")
    void getProofs() throws Exception {
        given(spaceFileService.getProofs(PROVIDER_ID, 10L)).willReturn(List.of(proof()));

        mockMvc.perform(get("/api/v1/provider/spaces/{spaceId}/proofs", 10L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isOk())
                .andDo(document("provider-space-proof-list",
                        resource(TAG, "권한 증빙 목록", "임시 주소는 10분 뒤 만료되므로 화면을 열 때마다 조회하세요."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        responseFields(success(proofFields("data[].")))));
    }

    @Test
    @DisplayName("권한 증빙 삭제")
    void deleteProof() throws Exception {
        mockMvc.perform(delete("/api/v1/provider/spaces/{spaceId}/proofs/{proofId}", 10L, 5L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isOk())
                .andDo(document("provider-space-proof-delete",
                        resource(TAG, "권한 증빙 삭제", "공개·일시 중단 중인 공간은 다시 검수 대기가 됩니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID"),
                                parameterWithName("proofId").description("증빙 ID")),
                        responseFields(success())));
        then(spaceFileService).should().deleteProof(PROVIDER_ID, 10L, 5L);
    }

    @Test
    @DisplayName("내 공간 목록: 검수 상태·사유와 대표 사진")
    void getMySpaces() throws Exception {
        ProviderSpaceSummaryResponse summary = new ProviderSpaceSummaryResponse(10L, "정왕역 앞 1층 상가",
                Region.JEONGWANG, SpaceStatus.REVISION_REQUESTED, "건물·호실이 증빙과 다릅니다.",
                photos().get(0).url(), 50_000, 2, 1, 14, FixedClockConfig.FIXED_LOCAL_NOW);
        given(spaceService.getMySpaces(eq(PROVIDER_ID), eq(SpaceStatus.REVISION_REQUESTED), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(summary), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/provider/spaces")
                        .param("status", "REVISION_REQUESTED")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reviewComment").value("건물·호실이 증빙과 다릅니다."))
                .andDo(document("provider-space-list",
                        resource(TAG, "내 공간 목록", "최근 수정 순. status 를 생략하면 전체 상태를 보여줍니다."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("status").optional().description("공개 상태 필터 (선택)"),
                                parameterWithName("page").optional().description("페이지 (0부터, 기본 0)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 20, 최대 100)")),
                        responseFields(page(
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("공간 ID"),
                                fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("공간 이름"),
                                fieldWithPath("data.content[].region").type(JsonFieldType.STRING).description("서비스 지역"),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("공개 상태"),
                                fieldWithPath("data.content[].reviewComment").type(JsonFieldType.STRING).optional().description("가장 최근 검수 사유"),
                                fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING).optional().description("대표 사진 주소"),
                                fieldWithPath("data.content[].dailyPrice").type(JsonFieldType.NUMBER).description("1일 이용료(원)"),
                                fieldWithPath("data.content[].photoCount").type(JsonFieldType.NUMBER).description("사진 수"),
                                fieldWithPath("data.content[].proofCount").type(JsonFieldType.NUMBER).description("증빙 수"),
                                fieldWithPath("data.content[].availableDateCount").type(JsonFieldType.NUMBER).description("사용 가능 날짜 수"),
                                fieldWithPath("data.content[].updatedAt").type(JsonFieldType.STRING).description("마지막 수정 시각")))));
    }

    @Test
    @DisplayName("내 공간 상세: 사진·증빙 포함")
    void getMySpace() throws Exception {
        given(spaceService.getMySpace(PROVIDER_ID, 10L)).willReturn(providerSpaceWithFiles());

        mockMvc.perform(get("/api/v1/provider/spaces/{spaceId}", 10L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.photos.length()").value(2))
                .andDo(document("provider-space-detail",
                        resource(TAG, "내 공간 상세", "사진, 증빙(10분짜리 임시 주소), 검수 사유를 포함합니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        responseFields(success(detailFields()))));
    }

    @Test
    @DisplayName("공간 정보 수정")
    void update() throws Exception {
        given(spaceService.update(eq(PROVIDER_ID), eq(10L), any(SpaceRequest.class))).willReturn(providerSpace());

        mockMvc.perform(put("/api/v1/provider/spaces/{spaceId}", 10L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(SpaceFixtures.request(Set.of(TOMORROW)))))
                .andExpect(status().isOk())
                .andDo(document("provider-space-update",
                        resource(TAG, "공간 정보 수정", "공개·일시 중단 중에 주소·건물·호실·면적·용도(검수 항목)나 지역이 바뀌면 "
                                + "다시 검수 대기(PENDING_REVIEW)가 됩니다. 이름·소개·가격·시설·허용 활동만 바꾸면 공개가 유지됩니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        requestFields(SpaceDocumentation.spaceRequestFields()),
                        responseFields(success(providerSpaceFields()))));
    }

    @Test
    @DisplayName("사용 가능 날짜(캘린더) 설정")
    void changeAvailableDates() throws Exception {
        given(spaceService.changeAvailableDates(eq(PROVIDER_ID), eq(10L), any())).willReturn(providerSpace());

        mockMvc.perform(put("/api/v1/provider/spaces/{spaceId}/available-dates", 10L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dates": ["%s", "%s"]}
                                """.formatted(TOMORROW, TOMORROW.plusDays(1))))
                .andExpect(status().isOk())
                .andDo(document("provider-space-available-dates",
                        resource(TAG, "사용 가능 날짜 설정", "보낸 날짜 목록으로 통째로 바꿉니다. 빈 목록이면 모두 닫습니다. "
                                + "오늘 이전·1년 이후 날짜가 있으면 400 INVALID_AVAILABLE_DATE."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        requestFields(fieldWithPath("dates").type(JsonFieldType.ARRAY).description("사용 가능 날짜 yyyy-MM-dd")),
                        responseFields(success(providerSpaceFields()))));
    }

    @Test
    @DisplayName("공개 일시 중단 · 재개 · 다시 검수 요청")
    void statusActions() throws Exception {
        given(spaceService.pause(PROVIDER_ID, 10L)).willReturn(providerSpace());
        given(spaceService.resume(PROVIDER_ID, 10L)).willReturn(providerSpace());
        given(spaceService.resubmit(PROVIDER_ID, 10L)).willReturn(providerSpace());

        for (String[] action : new String[][]{
                {"pause", "공개 일시 중단", "공개(PUBLISHED) 중인 공간만 중단할 수 있으며, 아니면 409 INVALID_SPACE_STATUS. 중단 중에는 새 신청을 받지 않습니다."},
                {"resume", "공개 재개", "일시 중단(PAUSED)한 공간만 다시 공개할 수 있습니다."},
                {"resubmit", "다시 검수 요청", "수정 요청·공개 보류 상태에서만 요청할 수 있습니다."}}) {
            mockMvc.perform(post("/api/v1/provider/spaces/{spaceId}/" + action[0], 10L)
                            .header(HttpHeaders.AUTHORIZATION, providerToken()))
                    .andExpect(status().isOk())
                    .andDo(document("provider-space-" + action[0],
                            resource(TAG, action[1], action[2]),
                            requestHeaders(authorization()),
                            pathParameters(parameterWithName("spaceId").description("공간 ID")),
                            responseFields(success(providerSpaceFields()))));
        }
    }

    @Test
    @DisplayName("상태가 맞지 않으면 409 INVALID_SPACE_STATUS")
    void pauseInWrongStatus() throws Exception {
        given(spaceService.pause(PROVIDER_ID, 10L)).willThrow(
                new BusinessException(ErrorCode.INVALID_SPACE_STATUS, "공개 중인 공간만 일시 중단할 수 있습니다."));

        mockMvc.perform(post("/api/v1/provider/spaces/{spaceId}/pause", 10L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_SPACE_STATUS"))
                .andExpect(jsonPath("$.error.message").value("공개 중인 공간만 일시 중단할 수 있습니다."));
    }

    static ProviderSpaceResponse providerSpaceWithFiles() {
        return ProviderSpaceResponse.of(SpaceFixtures.space(10L, SpaceFixtures.provider(PROVIDER_ID),
                Set.of(TOMORROW), FixedClockConfig.FIXED_TODAY), photos(), List.of(proof()));
    }

    static FieldDescriptor[] detailFields() {
        List<FieldDescriptor> fields = new ArrayList<>(SpaceDocumentation.providerSpaceFields("data."));
        fields.addAll(SpaceDocumentation.photoFields("data.photos[]."));
        fields.addAll(SpaceDocumentation.proofFields("data.proofs[]."));
        return fields.toArray(FieldDescriptor[]::new);
    }

    static List<SpacePhotoResponse> photos() {
        return List.of(
                new SpacePhotoResponse(2L, "https://abc.supabase.co/storage/v1/object/public/space-photos/spaces/10/photos/b.jpg", 0),
                new SpacePhotoResponse(1L, "https://abc.supabase.co/storage/v1/object/public/space-photos/spaces/10/photos/a.jpg", 1));
    }

    static SpaceProofResponse proof() {
        return new SpaceProofResponse(5L, ProofType.LEASE_CONTRACT, "임대차계약서.pdf", "application/pdf", 204_800L,
                "https://abc.supabase.co/storage/v1/object/sign/private-files/spaces/10/proofs/c.pdf?token=t",
                FixedClockConfig.FIXED_LOCAL_NOW);
    }

    private static FieldDescriptor[] photoListFields() {
        return SpaceDocumentation.photoFields("data[].").toArray(FieldDescriptor[]::new);
    }

    private static FieldDescriptor[] proofFields(String prefix) {
        return SpaceDocumentation.proofFields(prefix).toArray(FieldDescriptor[]::new);
    }

    static ProviderSpaceResponse providerSpace() {
        return ProviderSpaceResponse.of(SpaceFixtures.space(10L, SpaceFixtures.provider(PROVIDER_ID),
                Set.of(TOMORROW, TOMORROW.plusDays(1)), FixedClockConfig.FIXED_TODAY), List.of(), List.of());
    }

    static FieldDescriptor[] providerSpaceFields() {
        List<FieldDescriptor> fields = new ArrayList<>(SpaceDocumentation.providerSpaceFields("data."));
        return fields.toArray(FieldDescriptor[]::new);
    }
}
