package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static com.dasisiheung.support.MultipartDocumentation.filePart;
import static com.dasisiheung.support.MultipartDocumentation.requestParts;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.member.ChangePasswordRequest;
import com.dasisiheung.dto.member.MyProfileResponse;
import com.dasisiheung.dto.member.UpdateProfileRequest;
import com.dasisiheung.entity.member.MemberStatus;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.MemberService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.TestFiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(MemberController.class)
class MemberControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Member";
    private static final String WITHDRAW_DESCRIPTION = "이메일·이름·연락처는 즉시 지워지고 다시 로그인할 수 없습니다. "
            + "비밀번호가 틀리면 400 PASSWORD_MISMATCH. 진행 중인 신청 · 예약(검토 중이거나 이용이 끝나지 않은 확정 예약, "
            + "공간 제공자는 받은 신청 포함)이 있으면 409 WITHDRAWAL_BLOCKED_BY_APPLICATION.";
    private static final String PROFILE_IMAGE_URL =
            "https://abc.supabase.co/storage/v1/object/public/space-photos/members/1/profile/a.jpg";
    private static final String PROFILE_IMAGE_DESCRIPTION = "multipart 파트 image 에 사진 한 장을 보냅니다. JPG·PNG·WEBP 5MB. "
            + "이전 사진은 지워집니다. 같은 주소가 게시글 · 댓글 글쓴이(author.profileImageUrl)와 "
            + "대화 상대(counterpart.profileImageUrl)에도 내려갑니다. 파일이 없으면 400 FILE_REQUIRED, "
            + "5MB 를 넘으면 413 FILE_TOO_LARGE, 형식이 다르면 400 UNSUPPORTED_FILE_TYPE.";
    private static final MyProfileResponse PROFILE = profile(PROFILE_IMAGE_URL);
    private static final MyProfileResponse PROFILE_WITHOUT_IMAGE = profile(null);

    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("내 정보 조회")
    void getMyProfile() throws Exception {
        given(memberService.getMyProfile(USER_ID)).willReturn(PROFILE);

        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("01012345678"))
                .andDo(document("member-me",
                        resource(TAG, "내 정보 조회", "로그인한 회원의 정보를 조회합니다."),
                        requestHeaders(authorization()),
                        responseFields(success(profileFields()))));
    }

    @Test
    @DisplayName("토큰 없이 호출하면 401 UNAUTHORIZED")
    void getMyProfileWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andDo(document("member-me-unauthorized",
                        resource(TAG, "내 정보 조회", "토큰 없이 호출한 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("내 정보 수정: 보낸 항목만 바뀐다")
    void updateMyProfile() throws Exception {
        given(memberService.updateMyProfile(eq(USER_ID), any(UpdateProfileRequest.class))).willReturn(PROFILE);

        mockMvc.perform(patch("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "김시흥", "phone": "010-1234-5678"}
                                """))
                .andExpect(status().isOk())
                .andDo(document("member-me-update",
                        resource(TAG, "내 정보 수정", "이름·휴대폰 번호 중 바꿀 값만 보냅니다. 둘 다 없으면 400 입니다."),
                        requestHeaders(authorization()),
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).optional().description("이름 (1~30자)"),
                                fieldWithPath("phone").type(JsonFieldType.STRING).optional().description("휴대폰 번호")),
                        responseFields(success(profileFields()))));
    }

    @Test
    @DisplayName("내 정보 수정: 아무 항목도 보내지 않으면 400")
    void updateMyProfileWithEmptyBody() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("프로필 사진 변경")
    void changeProfileImage() throws Exception {
        given(memberService.changeProfileImage(eq(USER_ID), any(MultipartFile.class))).willReturn(PROFILE);

        mockMvc.perform(multipart("/api/v1/members/me/profile-image")
                        .file(TestFiles.jpeg("image"))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").value(PROFILE_IMAGE_URL))
                .andDo(document("member-me-profile-image-change",
                        resource(TAG, "프로필 사진 변경", PROFILE_IMAGE_DESCRIPTION),
                        requestHeaders(authorization()),
                        requestParts(filePart("image", "프로필 사진 (JPG·PNG·WEBP 5MB)")),
                        responseFields(success(profileFields()))));
    }

    @Test
    @DisplayName("프로필 사진 변경: 이미지가 아니면 400 UNSUPPORTED_FILE_TYPE")
    void changeProfileImageWithUnsupportedFile() throws Exception {
        given(memberService.changeProfileImage(eq(USER_ID), any(MultipartFile.class)))
                .willThrow(new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE));

        mockMvc.perform(multipart("/api/v1/members/me/profile-image")
                        .file(TestFiles.fakeImage("image"))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"))
                .andDo(document("member-me-profile-image-unsupported",
                        resource(TAG, "프로필 사진 변경", PROFILE_IMAGE_DESCRIPTION),
                        responseFields(error())));
    }

    @Test
    @DisplayName("프로필 사진 변경: image 파트가 없으면 400 INVALID_INPUT")
    void changeProfileImageWithoutPart() throws Exception {
        mockMvc.perform(multipart("/api/v1/members/me/profile-image")
                        .file(TestFiles.jpeg("photo"))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("프로필 사진 삭제")
    void deleteProfileImage() throws Exception {
        given(memberService.deleteProfileImage(USER_ID)).willReturn(PROFILE_WITHOUT_IMAGE);

        mockMvc.perform(delete("/api/v1/members/me/profile-image").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist())
                .andDo(document("member-me-profile-image-delete",
                        resource(TAG, "프로필 사진 삭제", "기본 이미지로 돌아갑니다(profileImageUrl = null). 사진이 없어도 200 입니다."),
                        requestHeaders(authorization()),
                        responseFields(success(profileFields()))));
    }

    @Test
    @DisplayName("비밀번호 변경")
    void changePassword() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/password")
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ChangePasswordRequest("password123", "newPassword456"))))
                .andExpect(status().isOk())
                .andDo(document("member-me-password",
                        resource(TAG, "비밀번호 변경", "현재 비밀번호가 틀리면 400 PASSWORD_MISMATCH, "
                                + "새 비밀번호가 같으면 400 SAME_AS_CURRENT_PASSWORD 입니다. 변경 후 다른 기기는 다시 로그인해야 합니다."),
                        requestHeaders(authorization()),
                        requestFields(
                                fieldWithPath("currentPassword").type(JsonFieldType.STRING).description("현재 비밀번호"),
                                fieldWithPath("newPassword").type(JsonFieldType.STRING).description("새 비밀번호 (영문·숫자 포함 8~64자)")),
                        responseFields(success())));
        then(memberService).should().changePassword(eq(USER_ID), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("회원 탈퇴")
    void withdraw() throws Exception {
        mockMvc.perform(post("/api/v1/members/me/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andDo(document("member-me-withdraw", resource(TAG, "회원 탈퇴", WITHDRAW_DESCRIPTION),
                        requestHeaders(authorization()),
                        requestFields(fieldWithPath("password").type(JsonFieldType.STRING).description("본인 확인용 현재 비밀번호")),
                        responseFields(success())));
        then(memberService).should().withdraw(USER_ID, "password123");
    }

    @Test
    @DisplayName("회원 탈퇴: 비밀번호가 틀리면 400 PASSWORD_MISMATCH")
    void withdrawWithWrongPassword() throws Exception {
        willThrow(new BusinessException(ErrorCode.PASSWORD_MISMATCH)).given(memberService).withdraw(USER_ID, "wrong1234");

        mockMvc.perform(post("/api/v1/members/me/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "wrong1234"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_MISMATCH"))
                .andDo(document("member-me-withdraw-wrong-password", resource(TAG, "회원 탈퇴", WITHDRAW_DESCRIPTION),
                        responseFields(error())));
    }

    @Test
    @DisplayName("회원 탈퇴: 진행 중인 신청 · 예약이 있으면 409 WITHDRAWAL_BLOCKED_BY_APPLICATION")
    void withdrawBlockedByApplication() throws Exception {
        willThrow(new BusinessException(ErrorCode.WITHDRAWAL_BLOCKED_BY_APPLICATION))
                .given(memberService).withdraw(USER_ID, "password123");

        mockMvc.perform(post("/api/v1/members/me/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "password123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("WITHDRAWAL_BLOCKED_BY_APPLICATION"))
                .andDo(document("member-me-withdraw-blocked", resource(TAG, "회원 탈퇴", WITHDRAW_DESCRIPTION),
                        responseFields(error())));
    }

    private static MyProfileResponse profile(String profileImageUrl) {
        return new MyProfileResponse(USER_ID, "user@example.com", "김시흥", "01012345678", profileImageUrl,
                Role.USER, MemberStatus.ACTIVE, FixedClockConfig.FIXED_LOCAL_NOW);
    }

    private static FieldDescriptor[] profileFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("회원 ID"),
                fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
                fieldWithPath("data.phone").type(JsonFieldType.STRING).description("휴대폰 번호 (숫자만)"),
                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).optional()
                        .description("프로필 사진 주소 (없으면 null — 기본 이미지)"),
                fieldWithPath("data.role").type(JsonFieldType.STRING).description("역할: USER | PROVIDER | ADMIN"),
                fieldWithPath("data.status").type(JsonFieldType.STRING).description("상태: ACTIVE | SUSPENDED | WITHDRAWN"),
                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입 시각 (KST, yyyy-MM-ddTHH:mm:ss)")
        };
    }
}
