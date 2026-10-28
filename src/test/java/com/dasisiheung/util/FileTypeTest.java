package com.dasisiheung.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.dasisiheung.support.TestFiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileTypeTest {

    @Test
    @DisplayName("파일 앞부분 시그니처로 JPEG · PNG · WEBP · PDF 를 판별한다")
    void detectsBySignature() {
        assertThat(FileType.detect(TestFiles.jpegBytes())).contains(FileType.JPEG);
        assertThat(FileType.detect(TestFiles.pngBytes())).contains(FileType.PNG);
        assertThat(FileType.detect(TestFiles.webpBytes())).contains(FileType.WEBP);
        assertThat(FileType.detect(TestFiles.pdfBytes())).contains(FileType.PDF);
    }

    @Test
    @DisplayName("시그니처가 없거나 너무 짧으면 판별하지 않는다")
    void unknown() {
        assertThat(FileType.detect("hello world".getBytes())).isEmpty();
        assertThat(FileType.detect(new byte[]{(byte) 0xFF})).isEmpty();
        assertThat(FileType.detect(new byte[0])).isEmpty();
        assertThat(FileType.detect(null)).isEmpty();
    }

    @Test
    @DisplayName("RIFF 로 시작해도 WEBP 표식이 없으면 WEBP 가 아니다 (예: WAV)")
    void riffWithoutWebpMarker() {
        byte[] wav = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'};

        assertThat(FileType.detect(wav)).isEmpty();
    }
}
