package com.dasisiheung.support;

import com.dasisiheung.support.MultipartDocumentation.PartType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.restdocs.RestDocumentationContext;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.operation.OperationRequestPart;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.request.RequestPartDescriptor;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.restdocs.snippet.SnippetException;

/**
 * 표준 request-parts 스니펫을 만들고, 같은 파트 정보를 명세용 request-parts.json 으로도 남긴다.
 * 파트 종류(글 / 파일)가 실제로 보낸 요청과 맞는지 검증한다.
 */
final class OpenApiRequestPartsSnippet implements Snippet {

    static final String FILE_NAME = "request-parts.json";

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final List<RequestPartDescriptor> descriptors;
    private final Snippet standardSnippet;

    OpenApiRequestPartsSnippet(RequestPartDescriptor... descriptors) {
        this.descriptors = List.of(descriptors);
        this.standardSnippet = RequestDocumentation.requestParts(descriptors);
    }

    @Override
    public void document(Operation operation) throws IOException {
        standardSnippet.document(operation);
        List<PartSpec> parts = descriptors.stream().map(this::toSpec).toList();
        verifyAgainstRequest(parts, operation.getRequest().getParts());
        write(operation, parts);
    }

    private PartSpec toSpec(RequestPartDescriptor descriptor) {
        if (!(descriptor.getAttributes().get(MultipartDocumentation.PART_TYPE) instanceof PartType type)) {
            throw new SnippetException("파트 '%s' 의 종류가 없습니다. MultipartDocumentation 의 textPart · filePart · filesPart 로 만드세요."
                    .formatted(descriptor.getName()));
        }
        return new PartSpec(descriptor.getName(), String.valueOf(descriptor.getDescription()), descriptor.isOptional(),
                type);
    }

    /** 문서에 적은 종류와 실제로 보낸 파트가 다르면 실패시킨다. (보내지 않은 선택 파트는 검사하지 않음) */
    private static void verifyAgainstRequest(List<PartSpec> parts, Collection<OperationRequestPart> sent) {
        Map<String, List<OperationRequestPart>> sentByName = sent.stream()
                .collect(Collectors.groupingBy(OperationRequestPart::getName));
        Map<String, PartSpec> specByName = parts.stream()
                .collect(Collectors.toMap(PartSpec::name, Function.identity()));
        sentByName.forEach((name, sentParts) -> {
            PartSpec spec = specByName.get(name);
            if (spec == null) {
                return; // 문서에 없는 파트는 표준 스니펫이 이미 실패시킨다.
            }
            boolean sentAsFile = sentParts.stream().allMatch(part -> part.getSubmittedFileName() != null);
            if (spec.type().isFile() != sentAsFile) {
                throw new SnippetException("파트 '%s' 는 %s 로 문서화했지만 실제로는 %s 로 보냈습니다."
                        .formatted(name, spec.type(), sentAsFile ? "파일" : "글"));
            }
            if (spec.type() == PartType.FILE && sentParts.size() > 1) {
                throw new SnippetException("파트 '%s' 를 여러 개 보냈습니다. 여러 파일이면 filesPart 로 문서화하세요.".formatted(name));
            }
        });
    }

    private static void write(Operation operation, List<PartSpec> parts) throws IOException {
        RestDocumentationContext context = (RestDocumentationContext) operation.getAttributes()
                .get(RestDocumentationContext.class.getName());
        Path directory = context.getOutputDirectory().toPath().resolve(operation.getName());
        Files.createDirectories(directory);
        MAPPER.writeValue(directory.resolve(FILE_NAME).toFile(), Map.of("parts", parts));
    }

    /** request-parts.json 의 파트 한 줄. type: TEXT · FILE · FILES */
    record PartSpec(String name, String description, boolean optional, PartType type) {
    }
}
