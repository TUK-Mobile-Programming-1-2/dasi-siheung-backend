package com.dasisiheung.util;

import java.util.Arrays;
import java.util.Optional;

/**
 * 파일 형식을 확장자·Content-Type 이 아닌 <b>파일 앞부분의 시그니처(magic number)</b>로 판별한다.
 * 확장자만 바꾼 실행 파일 등을 걸러내기 위함이다.
 */
public enum FileType {

    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    PDF("application/pdf", "pdf");

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_MARKER = {'W', 'E', 'B', 'P'};
    private static final int WEBP_MARKER_OFFSET = 8;
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};

    private final String contentType;
    private final String extension;

    FileType(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public boolean isImage() {
        return this != PDF;
    }

    public static Optional<FileType> detect(byte[] content) {
        if (content == null) {
            return Optional.empty();
        }
        if (startsWith(content, 0, JPEG_SIGNATURE)) {
            return Optional.of(JPEG);
        }
        if (startsWith(content, 0, PNG_SIGNATURE)) {
            return Optional.of(PNG);
        }
        if (startsWith(content, 0, RIFF) && startsWith(content, WEBP_MARKER_OFFSET, WEBP_MARKER)) {
            return Optional.of(WEBP);
        }
        if (startsWith(content, 0, PDF_SIGNATURE)) {
            return Optional.of(PDF);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] content, int offset, byte[] signature) {
        return content.length >= offset + signature.length
                && Arrays.equals(content, offset, offset + signature.length, signature, 0, signature.length);
    }
}
