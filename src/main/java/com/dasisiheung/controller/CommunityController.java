package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.community.CommentRequest;
import com.dasisiheung.dto.community.CommentResponse;
import com.dasisiheung.dto.community.PostDetailResponse;
import com.dasisiheung.dto.community.PostPhotoResponse;
import com.dasisiheung.dto.community.PostRequest;
import com.dasisiheung.dto.community.PostSummaryResponse;
import com.dasisiheung.entity.community.PostCategory;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.CommunityService;
import com.dasisiheung.service.PostPhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 커뮤니티. 조회(GET)는 로그인 없이, 쓰기는 로그인 회원. */
@Tag(name = "Community", description = "커뮤니티 게시글 · 댓글")
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final PostPhotoService postPhotoService;

    @Operation(summary = "게시글 목록", description = "최근 순. category · keyword(제목 · 본문) 로 거를 수 있습니다.")
    @GetMapping("/posts")
    public ApiResult<PageResponse<PostSummaryResponse>> getPosts(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(required = false) @Size(max = 50) String keyword,
            @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(communityService.getPosts(category, keyword, pageQuery));
    }

    @Operation(summary = "게시글 상세", description = "열 때마다 조회 수가 오릅니다. 토큰을 보내면 mine 에 내 글 여부가 담깁니다.")
    @GetMapping("/posts/{postId}")
    public ApiResult<PostDetailResponse> getPost(@LoginMember AuthMember loginMember, @PathVariable Long postId) {
        return ApiResult.ok(communityService.getPost(postId, loginMember == null ? null : loginMember.id()));
    }

    @Operation(summary = "게시글 쓰기",
            description = "공지(NOTICE)는 관리자만 쓸 수 있습니다. 사진은 응답의 id 로 '게시글 사진 추가'에 이어서 올립니다.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<PostDetailResponse> write(@LoginMember AuthMember loginMember,
                                               @Valid @RequestBody PostRequest request) {
        return ApiResult.ok(communityService.write(loginMember.id(), request));
    }

    @Operation(summary = "게시글 수정", description = "작성자만.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PutMapping("/posts/{postId}")
    public ApiResult<PostDetailResponse> edit(@LoginMember AuthMember loginMember, @PathVariable Long postId,
                                              @Valid @RequestBody PostRequest request) {
        return ApiResult.ok(communityService.edit(loginMember.id(), postId, request));
    }

    @Operation(summary = "게시글 사진 추가",
            description = "글을 쓴 뒤 받은 게시글 ID 로 올립니다. multipart 파트 files 에 여러 장. "
                    + "글당 최대 5장, JPG·PNG·WEBP 10MB. 작성자만. 응답은 전체 사진 목록(올린 순)입니다.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping(path = "/posts/{postId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<List<PostPhotoResponse>> addPhotos(@LoginMember AuthMember loginMember, @PathVariable Long postId,
                                                        @RequestPart("files") List<MultipartFile> files) {
        return ApiResult.ok(postPhotoService.addPhotos(loginMember.id(), postId, files));
    }

    @Operation(summary = "게시글 사진 삭제", description = "작성자만.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @DeleteMapping("/posts/{postId}/photos/{photoId}")
    public ApiResult<Void> deletePhoto(@LoginMember AuthMember loginMember, @PathVariable Long postId,
                                       @PathVariable Long photoId) {
        postPhotoService.deletePhoto(loginMember.id(), postId, photoId);
        return ApiResult.ok();
    }

    @Operation(summary = "게시글 삭제", description = "작성자 또는 관리자. 댓글 · 사진도 함께 지워집니다.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @DeleteMapping("/posts/{postId}")
    public ApiResult<Void> delete(@LoginMember AuthMember loginMember, @PathVariable Long postId) {
        communityService.delete(loginMember.id(), loginMember.role(), postId);
        return ApiResult.ok();
    }

    @Operation(summary = "댓글 목록", description = "오래된 순.")
    @GetMapping("/posts/{postId}/comments")
    public ApiResult<PageResponse<CommentResponse>> getComments(@PathVariable Long postId,
                                                                @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(communityService.getComments(postId, pageQuery));
    }

    @Operation(summary = "댓글 쓰기")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<CommentResponse> writeComment(@LoginMember AuthMember loginMember, @PathVariable Long postId,
                                                   @Valid @RequestBody CommentRequest request) {
        return ApiResult.ok(communityService.writeComment(loginMember.id(), postId, request));
    }

    @Operation(summary = "댓글 삭제", description = "작성자 또는 관리자.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @DeleteMapping("/comments/{commentId}")
    public ApiResult<Void> deleteComment(@LoginMember AuthMember loginMember, @PathVariable Long commentId) {
        communityService.deleteComment(loginMember.id(), loginMember.role(), commentId);
        return ApiResult.ok();
    }
}
