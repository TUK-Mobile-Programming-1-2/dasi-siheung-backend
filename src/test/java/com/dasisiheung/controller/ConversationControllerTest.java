package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.message.ConversationResponse;
import com.dasisiheung.dto.message.MessageRequest;
import com.dasisiheung.dto.message.MessageResponse;
import com.dasisiheung.dto.message.ReadRequest;
import com.dasisiheung.dto.message.UnreadCountResponse;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.service.ConversationService;
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

@ApiDocsTest(ConversationController.class)
class ConversationControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Conversation";

    @MockitoBean
    private ConversationService conversationService;

    @Test
    @DisplayName("대화 열기 · 내 대화 목록 · 읽음 처리")
    void conversations() throws Exception {
        given(conversationService.open(USER_ID, 10L)).willReturn(conversation(0));
        given(conversationService.getMyConversations(eq(USER_ID), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(conversation(2)), 0, 20, 1, 1, false));
        given(conversationService.markRead(USER_ID, 1L, 42L)).willReturn(conversation(0));

        mockMvc.perform(post("/api/v1/spaces/{spaceId}/conversations", 10L).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("conversation-open",
                        resource(TAG, "대화 열기", "이용자(USER)가 공간 제공자에게 엽니다. 이미 있으면 그 대화를 돌려줍니다(멱등). "
                                + "새로 열 때 공개 공간이 아니면 404 SPACE_NOT_FOUND."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        responseFields(success(conversationFields("data.")))));

        mockMvc.perform(get("/api/v1/conversations").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("conversation-list",
                        resource(TAG, "내 대화 목록", "이용자 · 공간 제공자 모두. 최근 메시지 순, 메시지 없는 대화는 맨 뒤."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(conversationFields("data.content[].")))));

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/read", 1L).header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json(new ReadRequest(42L))))
                .andExpect(status().isOk())
                .andDo(document("conversation-read",
                        resource(TAG, "읽음 처리", "화면에 보인 마지막 메시지 ID 를 보냅니다. 읽음 위치는 뒤로 돌아가지 않습니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("conversationId").description("대화 ID")),
                        requestFields(fieldWithPath("lastMessageId").type(JsonFieldType.NUMBER).description("마지막으로 읽은 메시지 ID")),
                        responseFields(success(conversationFields("data.")))));
    }

    @Test
    @DisplayName("메시지 조회(폴링) · 보내기 · 안 읽은 수")
    void messages() throws Exception {
        MessageResponse message = new MessageResponse(43L, 1L, USER_ID, "이이용", true,
                "안녕하세요! 11월 첫 주말에 팝업 가능할까요?", FixedClockConfig.FIXED_LOCAL_NOW);
        given(conversationService.getMessages(USER_ID, 1L, 42L, null, 50)).willReturn(List.of(message));
        given(conversationService.send(USER_ID, 1L, "안녕하세요! 11월 첫 주말에 팝업 가능할까요?")).willReturn(message);
        given(conversationService.getUnreadCount(USER_ID)).willReturn(new UnreadCountResponse(3));

        mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", 1L).param("after", "42")
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("conversation-messages",
                        resource(TAG, "메시지 조회 (폴링)", "대화방에서는 5초마다 after=마지막 메시지 ID 로 새 메시지만 받습니다. "
                                + "위로 스크롤하면 before=첫 메시지 ID 로 지난 메시지를 받습니다. 결과는 항상 오래된 순."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("conversationId").description("대화 ID")),
                        queryParameters(
                                parameterWithName("after").optional().description("이 ID 뒤의 새 메시지"),
                                parameterWithName("before").optional().description("이 ID 앞의 지난 메시지"),
                                parameterWithName("size").optional().description("최대 개수 (기본 50, 최대 100)")),
                        responseFields(success(messageFields("data[].")))));

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", 1L).header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new MessageRequest("안녕하세요! 11월 첫 주말에 팝업 가능할까요?"))))
                .andExpect(status().isCreated())
                .andDo(document("conversation-send",
                        resource(TAG, "메시지 보내기", "대화 참여자만 (아니면 403 NOT_CONVERSATION_MEMBER)."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("conversationId").description("대화 ID")),
                        requestFields(fieldWithPath("content").type(JsonFieldType.STRING).description("메시지 (최대 1000자)")),
                        responseFields(success(messageFields("data.")))));

        mockMvc.perform(get("/api/v1/conversations/unread-count").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("conversation-unread-count",
                        resource(TAG, "안 읽은 메시지 수", "앱 탭 배지용. 30초마다 조회하세요."),
                        requestHeaders(authorization()),
                        responseFields(success(fieldWithPath("data.count").type(JsonFieldType.NUMBER)
                                .description("모든 대화의 안 읽은 메시지 수")))));
    }

    @Test
    @DisplayName("공간 제공자는 대화를 열 수 없다 (403)")
    void providerCannotOpen() throws Exception {
        mockMvc.perform(post("/api/v1/spaces/{spaceId}/conversations", 10L).header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isForbidden());
    }

    private static ConversationResponse conversation(long unread) {
        return new ConversationResponse(1L, 10L, "정왕역 앞 1층 상가", ProviderSpaceControllerTest.photos().get(0).url(),
                new ConversationResponse.Counterpart(PROVIDER_ID, "김제공", Role.PROVIDER),
                "네 가능합니다. 신청서 보내 주세요.", FixedClockConfig.FIXED_LOCAL_NOW, unread, 40L);
    }

    private static FieldDescriptor[] conversationFields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("대화 ID"),
                fieldWithPath(prefix + "spaceId").type(JsonFieldType.NUMBER).description("공간 ID"),
                fieldWithPath(prefix + "spaceTitle").type(JsonFieldType.STRING).description("공간 이름"),
                fieldWithPath(prefix + "spaceThumbnailUrl").type(JsonFieldType.STRING).optional().description("공간 대표 사진 주소"),
                fieldWithPath(prefix + "counterpart.id").type(JsonFieldType.NUMBER).description("상대방 회원 ID"),
                fieldWithPath(prefix + "counterpart.name").type(JsonFieldType.STRING).description("상대방 이름"),
                fieldWithPath(prefix + "counterpart.role").type(JsonFieldType.STRING).description("상대방 역할"),
                fieldWithPath(prefix + "lastMessagePreview").type(JsonFieldType.STRING).optional().description("마지막 메시지 미리보기"),
                fieldWithPath(prefix + "lastMessageAt").type(JsonFieldType.STRING).optional().description("마지막 메시지 시각"),
                fieldWithPath(prefix + "unreadCount").type(JsonFieldType.NUMBER).description("안 읽은 메시지 수"),
                fieldWithPath(prefix + "myLastReadMessageId").type(JsonFieldType.NUMBER).description("내가 마지막으로 읽은 메시지 ID")};
    }

    private static FieldDescriptor[] messageFields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("메시지 ID (클수록 최근)"),
                fieldWithPath(prefix + "conversationId").type(JsonFieldType.NUMBER).description("대화 ID"),
                fieldWithPath(prefix + "senderId").type(JsonFieldType.NUMBER).description("보낸 회원 ID"),
                fieldWithPath(prefix + "senderName").type(JsonFieldType.STRING).description("보낸 회원 이름"),
                fieldWithPath(prefix + "mine").type(JsonFieldType.BOOLEAN).description("내가 보냈는지"),
                fieldWithPath(prefix + "content").type(JsonFieldType.STRING).description("내용"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("보낸 시각")};
    }
}
