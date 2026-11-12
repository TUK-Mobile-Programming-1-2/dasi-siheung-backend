package com.dasisiheung.push;

import java.util.List;

/**
 * @param sentCount     보낸 토큰 수
 * @param invalidTokens 더 이상 쓸 수 없는 토큰 (앱 삭제 · 토큰 만료 등) — 지워야 한다
 */
public record PushResult(int sentCount, List<String> invalidTokens) {

    public PushResult {
        invalidTokens = List.copyOf(invalidTokens);
    }

    public static PushResult none() {
        return new PushResult(0, List.of());
    }
}
