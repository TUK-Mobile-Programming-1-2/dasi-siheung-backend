package com.dasisiheung.support;

import java.util.Arrays;
import org.springframework.mock.web.MockMultipartFile;

/** 형식 시그니처만 맞춘 작은 테스트 파일. */
public final class TestFiles {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7', '\n'};

    private TestFiles() {
    }

    public static byte[] jpegBytes() {
        return Arrays.copyOf(JPEG, JPEG.length);
    }

    public static byte[] pngBytes() {
        return Arrays.copyOf(PNG, PNG.length);
    }

    public static byte[] webpBytes() {
        return Arrays.copyOf(WEBP, WEBP.length);
    }

    public static byte[] pdfBytes() {
        return Arrays.copyOf(PDF, PDF.length);
    }

    public static MockMultipartFile jpeg(String partName) {
        return new MockMultipartFile(partName, "photo.jpg", "image/jpeg", jpegBytes());
    }

    public static MockMultipartFile pdf(String partName) {
        return new MockMultipartFile(partName, "proof.pdf", "application/pdf", pdfBytes());
    }

    /** 확장자·Content-Type 은 이미지인 척하지만 내용은 텍스트인 파일. */
    public static MockMultipartFile fakeImage(String partName) {
        return new MockMultipartFile(partName, "photo.jpg", "image/jpeg", "not an image".getBytes());
    }
}
