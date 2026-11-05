package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
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
import com.dasisiheung.dto.review.ReviewReplyRequest;
import com.dasisiheung.dto.review.ReviewRequest;
import com.dasisiheung.dto.review.ReviewResponse;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.ReviewService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(ReviewController.class)
class ReviewControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Review";

    @MockitoBean
    private ReviewService reviewService;

    @Test
    @DisplayName("공간 후기 목록 (로그인 없이)")
    void getSpaceReviews() throws Exception {
        given(reviewService.getSpaceReviews(eq(10L), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(review()), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/spaces/{spaceId}/reviews", 10L))
                .andExpect(status().isOk())
                .andDo(document("review-space-list",
                        resource(TAG, "공간 후기 목록", "공개 공간의 후기 (최근 순). 평균 평점 · 후기 수는 공간 상세에 있습니다."),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        queryParameters(
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(fields("data.content[].")))));
    }

    @Test
    @DisplayName("후기 작성")
    void write() throws Exception {
        given(reviewService.write(eq(USER_ID), eq(7L), any(ReviewRequest.class))).willReturn(review());

        mockMvc.perform(post("/api/v1/applications/{applicationId}/review", 7L)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewRequest(5, "역 앞이라 유동 인구가 많아 첫날부터 매출이 좋았어요."))))
                .andExpect(status().isCreated())
                .andDo(document("review-write",
                        resource(TAG, "후기 작성", "이용이 끝난(종료일이 지난) 확정 예약의 신청자만 쓸 수 있습니다 "
                                + "(아니면 409 REVIEW_NOT_ALLOWED). 예약 1건당 1번 (409 DUPLICATE_REVIEW)."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("applicationId").description("신청(예약) ID")),
                        requestFields(requestFieldsOf()),
                        responseFields(success(fields("data.")))));
    }

    @Test
    @DisplayName("후기 작성: 아직 이용이 끝나지 않았으면 409 REVIEW_NOT_ALLOWED")
    void writeTooEarly() throws Exception {
        given(reviewService.write(eq(USER_ID), eq(7L), any(ReviewRequest.class)))
                .willThrow(new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED));

        mockMvc.perform(post("/api/v1/applications/{applicationId}/review", 7L)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewRequest(5, "좋아요"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_ALLOWED"))
                .andDo(document("review-write-not-allowed", resource(TAG, "후기 작성", "이용 전 작성 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("후기 수정 · 삭제")
    void editAndDelete() throws Exception {
        given(reviewService.edit(eq(USER_ID), eq(3L), any(ReviewRequest.class))).willReturn(review());

        mockMvc.perform(put("/api/v1/reviews/{reviewId}", 3L)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewRequest(4, "다시 생각해 보니 주차가 조금 불편했어요."))))
                .andExpect(status().isOk())
                .andDo(document("review-edit",
                        resource(TAG, "후기 수정", "본인 후기만 (아니면 403 NOT_REVIEW_OWNER). 공간 평균 평점이 다시 계산됩니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("reviewId").description("후기 ID")),
                        requestFields(requestFieldsOf()),
                        responseFields(success(fields("data.")))));

        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", 3L).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("review-delete",
                        resource(TAG, "후기 삭제", "본인 후기만. 공간 후기 수 · 평균 평점에서 빠집니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("reviewId").description("후기 ID")),
                        responseFields(fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부 (true)"))));
    }

    @Test
    @DisplayName("후기 답글 (공간 제공자)")
    void reply() throws Exception {
        given(reviewService.reply(eq(PROVIDER_ID), eq(3L), any(ReviewReplyRequest.class))).willReturn(review());

        mockMvc.perform(post("/api/v1/reviews/{reviewId}/reply", 3L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewReplyRequest("깨끗하게 써 주셔서 감사합니다."))))
                .andExpect(status().isOk())
                .andDo(document("review-reply",
                        resource(TAG, "후기 답글 (공간 제공자)", "내 공간의 후기에만 (아니면 403 NOT_SPACE_OWNER). 다시 보내면 고칩니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("reviewId").description("후기 ID")),
                        requestFields(fieldWithPath("content").type(JsonFieldType.STRING).description("답글 (최대 500자)")),
                        responseFields(success(fields("data.")))));
    }

    @Test
    @DisplayName("받은 후기 (공간 제공자) — 답글 대기만")
    void getReceivedReviews() throws Exception {
        given(reviewService.getReceivedReviews(eq(PROVIDER_ID), eq(10L), eq(false), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(waitingReview()), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/provider/reviews").param("spaceId", "10").param("replied", "false")
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isOk())
                .andDo(document("review-received-list",
                        resource(TAG, "받은 후기 (공간 제공자)", "내 모든 공간의 후기 (최근 순). 답글을 달 후기를 모아 보는 화면용. "
                                + "답글 대기 개수는 replied=false&size=1 의 totalElements 로 알 수 있습니다. 이용자는 403."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("spaceId").optional().description("특정 공간만 (없으면 내 모든 공간)"),
                                parameterWithName("replied").optional()
                                        .description("true 답글 단 후기만 | false 답글을 기다리는 후기만 | 없으면 전체"),
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(fields("data.content[].")))));
    }

    @Test
    @DisplayName("받은 후기: 이용자는 볼 수 없다 (403)")
    void userCannotSeeReceivedReviews() throws Exception {
        mockMvc.perform(get("/api/v1/provider/reviews").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내가 쓴 후기")
    void getMyReviews() throws Exception {
        given(reviewService.getMyReviews(eq(USER_ID), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(review()), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/members/me/reviews").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("review-my-list", resource(TAG, "내가 쓴 후기", "최근 순"),
                        requestHeaders(authorization()),
                        responseFields(page(fields("data.content[].")))));
    }

    private static ReviewResponse review() {
        return new ReviewResponse(3L, 10L, "정왕역 앞 1층 상가", 7L, "이이용", ActivityType.POPUP_STORE,
                FixedClockConfig.FIXED_TODAY.minusDays(3), FixedClockConfig.FIXED_TODAY.minusDays(2), 5,
                "역 앞이라 유동 인구가 많아 첫날부터 매출이 좋았어요.", "감사합니다!", FixedClockConfig.FIXED_LOCAL_NOW,
                FixedClockConfig.FIXED_LOCAL_NOW, FixedClockConfig.FIXED_LOCAL_NOW);
    }

    private static ReviewResponse waitingReview() {
        return new ReviewResponse(4L, 10L, "정왕역 앞 1층 상가", 8L, "박전시", ActivityType.EXHIBITION,
                FixedClockConfig.FIXED_TODAY.minusDays(5), FixedClockConfig.FIXED_TODAY.minusDays(4), 4,
                "채광이 좋아 작품이 잘 보였어요. 주차만 조금 아쉬웠습니다.", null, null,
                FixedClockConfig.FIXED_LOCAL_NOW, FixedClockConfig.FIXED_LOCAL_NOW);
    }

    private static List<FieldDescriptor> requestFieldsOf() {
        return List.of(
                fieldWithPath("rating").type(JsonFieldType.NUMBER).description("평점 (1~5)"),
                fieldWithPath("content").type(JsonFieldType.STRING).description("후기 (최대 1000자)"));
    }

    private static FieldDescriptor[] fields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("후기 ID"),
                fieldWithPath(prefix + "spaceId").type(JsonFieldType.NUMBER).description("공간 ID"),
                fieldWithPath(prefix + "spaceTitle").type(JsonFieldType.STRING).description("공간 이름"),
                fieldWithPath(prefix + "applicationId").type(JsonFieldType.NUMBER).description("신청(예약) ID"),
                fieldWithPath(prefix + "reviewerName").type(JsonFieldType.STRING).description("작성자 이름"),
                fieldWithPath(prefix + "activityType").type(JsonFieldType.STRING).description("활동 종류"),
                fieldWithPath(prefix + "startDate").type(JsonFieldType.STRING).description("이용 시작일"),
                fieldWithPath(prefix + "endDate").type(JsonFieldType.STRING).description("이용 종료일"),
                fieldWithPath(prefix + "rating").type(JsonFieldType.NUMBER).description("평점 (1~5)"),
                fieldWithPath(prefix + "content").type(JsonFieldType.STRING).description("후기"),
                fieldWithPath(prefix + "reply").type(JsonFieldType.STRING).optional().description("공간 제공자 답글"),
                fieldWithPath(prefix + "repliedAt").type(JsonFieldType.STRING).optional().description("답글 시각"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                fieldWithPath(prefix + "updatedAt").type(JsonFieldType.STRING).description("마지막 수정 시각")};
    }
}
