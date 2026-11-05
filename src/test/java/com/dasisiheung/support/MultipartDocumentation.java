package com.dasisiheung.support;

import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.snippet.Attributes.key;

import org.springframework.restdocs.request.RequestPartDescriptor;
import org.springframework.restdocs.snippet.Snippet;

/**
 * multipart/form-data 요청 문서화 도우미.
 * <p>
 * epages openapi3 는 REST Docs 의 requestParts 를 명세(requestBody)로 옮기지 않는다. 그래서 파트마다 종류(글 · 파일 · 여러 파일)를
 * 밝히게 하고, 표준 request-parts 스니펫과 함께 명세용 request-parts.json 을 남긴다.
 * build.gradle 의 openapi3 후처리(gradle/openapi-multipart.gradle)가 이 파일로 requestBody 를 채운다.
 * <pre>
 * .andDo(document("report-create", resource(...),
 *         requestParts(textPart("reason", "사유"), filesPart("photos", "증거 사진").optional())))
 * </pre>
 */
public final class MultipartDocumentation {

    static final String PART_TYPE = "partType";

    private MultipartDocumentation() {
    }

    /** 글 파트 (string). */
    public static RequestPartDescriptor textPart(String name, String description) {
        return part(name, description, PartType.TEXT);
    }

    /** 파일 한 개 파트 (binary). */
    public static RequestPartDescriptor filePart(String name, String description) {
        return part(name, description, PartType.FILE);
    }

    /** 같은 이름으로 여러 번 보내는 파일 파트 (binary 배열). */
    public static RequestPartDescriptor filesPart(String name, String description) {
        return part(name, description, PartType.FILES);
    }

    /** request-parts 스니펫 + 명세용 request-parts.json. 파트 종류가 실제 요청과 다르면 테스트가 실패한다. */
    public static Snippet requestParts(RequestPartDescriptor... descriptors) {
        return new OpenApiRequestPartsSnippet(descriptors);
    }

    private static RequestPartDescriptor part(String name, String description, PartType type) {
        return partWithName(name).description(description).attributes(key(PART_TYPE).value(type));
    }

    /** 파트 종류. */
    public enum PartType {
        TEXT, FILE, FILES;

        boolean isFile() {
            return this != TEXT;
        }
    }
}
