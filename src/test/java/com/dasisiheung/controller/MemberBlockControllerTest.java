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
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.member.BlockedMemberResponse;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.MemberBlockService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(MemberBlockController.class)
class MemberBlockControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Block";
    private static final BlockedMemberResponse BLOCKED = new BlockedMemberResponse(PROVIDER_ID, "시흥러버",
            "https://abc.supabase.co/storage/v1/object/public/space-photos/members/2/profile/a.jpg",
            FixedClockConfig.FIXED_LOCAL_NOW);

    @MockitoBean
    private MemberBlockService memberBlockService;

    @Test
    @DisplayName("차단하기 · 차단한 사용자 목록 · 차단 해제")
    void blocks() throws Exception {
        given(memberBlockService.block(USER_ID, PROVIDER_ID)).willReturn(BLOCKED);
        given(memberBlockService.getMyBlocks(eq(USER_ID), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(BLOCKED), 0, 20, 1, 1, false));

        mockMvc.perform(put("/api/v1/members/me/blocks/{memberId}", PROVIDER_ID).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(PROVIDER_ID))
                .andDo(document("block-add",
                        resource(TAG, "차단하기", "이미 차단했으면 그대로 두고 처음 차단한 시각을 돌려줍니다(멱등). "
                                + "차단하면 서로 메시지를 보내거나 새 대화를 열 수 없고(403 BLOCKED_MEMBER) 상대의 메시지 푸시도 "
                                + "오지 않습니다. 이미 있는 대화방 · 메시지는 그대로 보입니다. "
                                + "본인이면 400 CANNOT_BLOCK_SELF, 없거나 탈퇴한 회원이면 404 MEMBER_NOT_FOUND."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("memberId")
                                .description("차단할 회원 ID (대화의 counterpart.id, 글 · 댓글의 author.id)")),
                        responseFields(success(blockedFields("data.")))));

        mockMvc.perform(get("/api/v1/members/me/blocks").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("block-list",
                        resource(TAG, "차단한 사용자 목록", "최근 차단 순. 탈퇴한 회원도 남아 해제할 수 있습니다 (이름은 \"탈퇴한 회원\")."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(blockedFields("data.content[].")))));

        mockMvc.perform(delete("/api/v1/members/me/blocks/{memberId}", PROVIDER_ID).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("block-remove",
                        resource(TAG, "차단 해제", "차단하지 않은 상대여도 성공합니다(멱등)."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("memberId").description("차단을 풀 회원 ID")),
                        responseFields(fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부 (true)"))));
        then(memberBlockService).should().unblock(USER_ID, PROVIDER_ID);
    }

    @Test
    @DisplayName("자기 자신을 차단하면 400 CANNOT_BLOCK_SELF")
    void cannotBlockSelf() throws Exception {
        given(memberBlockService.block(USER_ID, USER_ID)).willThrow(new BusinessException(ErrorCode.CANNOT_BLOCK_SELF));

        mockMvc.perform(put("/api/v1/members/me/blocks/{memberId}", USER_ID).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CANNOT_BLOCK_SELF"))
                .andDo(document("block-add-self", resource(TAG, "차단하기", "자기 자신을 차단한 예시"), responseFields(error())));
    }

    @Test
    @DisplayName("로그인이 필요하다 (401)")
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/blocks")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/members/me/blocks/{memberId}", PROVIDER_ID)).andExpect(status().isUnauthorized());
        then(memberBlockService).shouldHaveNoInteractions();
    }

    private static FieldDescriptor[] blockedFields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER)
                        .description("차단한 회원 ID (대화의 counterpart.id, 글 · 댓글의 author.id 와 같은 값)"),
                fieldWithPath(prefix + "name").type(JsonFieldType.STRING).description("이름 (탈퇴했으면 \"탈퇴한 회원\")"),
                fieldWithPath(prefix + "profileImageUrl").type(JsonFieldType.STRING).optional()
                        .description("프로필 사진 주소 (없거나 탈퇴했으면 null)"),
                fieldWithPath(prefix + "blockedAt").type(JsonFieldType.STRING).description("차단한 시각")};
    }
}
