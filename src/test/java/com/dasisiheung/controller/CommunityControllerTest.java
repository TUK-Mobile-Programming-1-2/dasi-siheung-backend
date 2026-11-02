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
import com.dasisiheung.dto.community.AuthorInfo;
import com.dasisiheung.dto.community.CommentRequest;
import com.dasisiheung.dto.community.CommentResponse;
import com.dasisiheung.dto.community.PostDetailResponse;
import com.dasisiheung.dto.community.PostRequest;
import com.dasisiheung.dto.community.PostSummaryResponse;
import com.dasisiheung.entity.community.PostCategory;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.CommunityService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(CommunityController.class)
class CommunityControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Community";
    private static final AuthorInfo AUTHOR = new AuthorInfo(USER_ID, "이이용", Role.USER);

    @MockitoBean
    private CommunityService communityService;

    @Test
    @DisplayName("게시글 목록 (로그인 없이)")
    void getPosts() throws Exception {
        given(communityService.getPosts(eq(PostCategory.RECRUIT), eq("플리마켓"), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(new PostSummaryResponse(1L, PostCategory.RECRUIT,
                        "배곧 플리마켓 같이 하실 분 구해요", AUTHOR, 3, 42, FixedClockConfig.FIXED_LOCAL_NOW)), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/community/posts").param("category", "RECRUIT").param("keyword", "플리마켓"))
                .andExpect(status().isOk())
                .andDo(document("community-post-list",
                        resource(TAG, "게시글 목록", "최근 순. 숨김(신고 처리)된 글은 나오지 않습니다."),
                        queryParameters(
                                parameterWithName("category").optional().description("분류 (선택지 postCategories)"),
                                parameterWithName("keyword").optional().description("제목 · 본문 검색어 (최대 50자)"),
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.content[].category").type(JsonFieldType.STRING).description("분류"),
                                fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.content[].author.id").type(JsonFieldType.NUMBER).description("글쓴이 회원 ID"),
                                fieldWithPath("data.content[].author.name").type(JsonFieldType.STRING).description("글쓴이 이름"),
                                fieldWithPath("data.content[].author.role").type(JsonFieldType.STRING).description("글쓴이 역할"),
                                fieldWithPath("data.content[].commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                                fieldWithPath("data.content[].viewCount").type(JsonFieldType.NUMBER).description("조회 수"),
                                fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("작성 시각")))));
    }

    @Test
    @DisplayName("게시글 상세 · 쓰기 · 수정 · 삭제")
    void postCrud() throws Exception {
        given(communityService.getPost(1L, null)).willReturn(detail(false));
        given(communityService.write(eq(USER_ID), any(PostRequest.class))).willReturn(detail(true));
        given(communityService.edit(eq(USER_ID), eq(1L), any(PostRequest.class))).willReturn(detail(true));
        PostRequest request = new PostRequest(PostCategory.RECRUIT, "배곧 플리마켓 같이 하실 분 구해요",
                "11월 둘째 주말, 핸드메이드 소품 팝업을 함께 열 팀을 찾습니다.");

        mockMvc.perform(get("/api/v1/community/posts/{postId}", 1L))
                .andExpect(status().isOk())
                .andDo(document("community-post-detail",
                        resource(TAG, "게시글 상세", "열 때마다 조회 수가 오릅니다. 토큰을 보내면 mine 에 내 글 여부가 담깁니다."),
                        pathParameters(parameterWithName("postId").description("게시글 ID")),
                        responseFields(success(detailFields()))));

        mockMvc.perform(post("/api/v1/community/posts").header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andDo(document("community-post-write",
                        resource(TAG, "게시글 쓰기", "로그인 회원 누구나. 공지(NOTICE)는 관리자만 (아니면 403 NOTICE_ADMIN_ONLY)."),
                        requestHeaders(authorization()),
                        requestFields(postRequestFields()),
                        responseFields(success(detailFields()))));

        mockMvc.perform(put("/api/v1/community/posts/{postId}", 1L).header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk())
                .andDo(document("community-post-edit",
                        resource(TAG, "게시글 수정", "작성자만 (아니면 403 NOT_POST_AUTHOR)."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("postId").description("게시글 ID")),
                        requestFields(postRequestFields()),
                        responseFields(success(detailFields()))));

        mockMvc.perform(delete("/api/v1/community/posts/{postId}", 1L).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("community-post-delete",
                        resource(TAG, "게시글 삭제", "작성자 또는 관리자. 댓글도 함께 지워집니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("postId").description("게시글 ID")),
                        responseFields(fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부 (true)"))));
    }

    @Test
    @DisplayName("공지를 일반 회원이 쓰면 403 NOTICE_ADMIN_ONLY")
    void noticeAdminOnly() throws Exception {
        given(communityService.write(eq(USER_ID), any(PostRequest.class)))
                .willThrow(new BusinessException(ErrorCode.NOTICE_ADMIN_ONLY));

        mockMvc.perform(post("/api/v1/community/posts").header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PostRequest(PostCategory.NOTICE, "공지", "내용"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOTICE_ADMIN_ONLY"))
                .andDo(document("community-post-write-notice-forbidden",
                        resource(TAG, "게시글 쓰기", "일반 회원이 공지를 쓴 예시"), responseFields(error())));
    }

    @Test
    @DisplayName("댓글 목록 · 쓰기 · 삭제")
    void comments() throws Exception {
        CommentResponse comment = new CommentResponse(5L, 1L, AUTHOR, "저도 관심 있어요!", FixedClockConfig.FIXED_LOCAL_NOW);
        given(communityService.getComments(eq(1L), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(comment), 0, 20, 1, 1, false));
        given(communityService.writeComment(eq(USER_ID), eq(1L), any(CommentRequest.class))).willReturn(comment);

        mockMvc.perform(get("/api/v1/community/posts/{postId}/comments", 1L))
                .andExpect(status().isOk())
                .andDo(document("community-comment-list",
                        resource(TAG, "댓글 목록", "오래된 순."),
                        pathParameters(parameterWithName("postId").description("게시글 ID")),
                        responseFields(page(commentFields("data.content[].")))));

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", 1L).header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json(new CommentRequest("저도 관심 있어요!"))))
                .andExpect(status().isCreated())
                .andDo(document("community-comment-write",
                        resource(TAG, "댓글 쓰기", "로그인 회원 누구나."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("postId").description("게시글 ID")),
                        requestFields(fieldWithPath("content").type(JsonFieldType.STRING).description("댓글 (최대 1000자)")),
                        responseFields(success(commentFields("data.")))));

        mockMvc.perform(delete("/api/v1/community/comments/{commentId}", 5L).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("community-comment-delete",
                        resource(TAG, "댓글 삭제", "작성자 또는 관리자 (아니면 403 NOT_COMMENT_AUTHOR)."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("commentId").description("댓글 ID")),
                        responseFields(fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부 (true)"))));
    }

    @Test
    @DisplayName("비로그인은 글을 쓸 수 없다 (401)")
    void writeRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PostRequest(PostCategory.QUESTION, "제목", "내용"))))
                .andExpect(status().isUnauthorized());
    }

    private static PostDetailResponse detail(boolean mine) {
        return new PostDetailResponse(1L, PostCategory.RECRUIT, "배곧 플리마켓 같이 하실 분 구해요",
                "11월 둘째 주말, 핸드메이드 소품 팝업을 함께 열 팀을 찾습니다.", AUTHOR, 3, 42, mine,
                FixedClockConfig.FIXED_LOCAL_NOW, FixedClockConfig.FIXED_LOCAL_NOW);
    }

    private static List<FieldDescriptor> postRequestFields() {
        return List.of(
                fieldWithPath("category").type(JsonFieldType.STRING).description("분류 (선택지 postCategories, 공지는 관리자만)"),
                fieldWithPath("title").type(JsonFieldType.STRING).description("제목 (최대 100자)"),
                fieldWithPath("content").type(JsonFieldType.STRING).description("본문 (최대 5000자)"));
    }

    private static FieldDescriptor[] detailFields() {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("게시글 ID"),
                fieldWithPath("data.category").type(JsonFieldType.STRING).description("분류"),
                fieldWithPath("data.title").type(JsonFieldType.STRING).description("제목"),
                fieldWithPath("data.content").type(JsonFieldType.STRING).description("본문"),
                fieldWithPath("data.author.id").type(JsonFieldType.NUMBER).description("글쓴이 회원 ID"),
                fieldWithPath("data.author.name").type(JsonFieldType.STRING).description("글쓴이 이름"),
                fieldWithPath("data.author.role").type(JsonFieldType.STRING).description("글쓴이 역할"),
                fieldWithPath("data.commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                fieldWithPath("data.viewCount").type(JsonFieldType.NUMBER).description("조회 수"),
                fieldWithPath("data.mine").type(JsonFieldType.BOOLEAN).description("내가 쓴 글인지"),
                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("마지막 수정 시각")));
        return fields.toArray(FieldDescriptor[]::new);
    }

    private static FieldDescriptor[] commentFields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("댓글 ID"),
                fieldWithPath(prefix + "postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                fieldWithPath(prefix + "author.id").type(JsonFieldType.NUMBER).description("글쓴이 회원 ID"),
                fieldWithPath(prefix + "author.name").type(JsonFieldType.STRING).description("글쓴이 이름"),
                fieldWithPath(prefix + "author.role").type(JsonFieldType.STRING).description("글쓴이 역할"),
                fieldWithPath(prefix + "content").type(JsonFieldType.STRING).description("댓글"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("작성 시각")};
    }
}
