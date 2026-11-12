package com.dasisiheung.push;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** FCM 키가 없을 때 쓴다. 실제로 보내지 않고 무엇을 보냈을지 로그로만 남긴다. */
@Slf4j
public class LoggingPushSender implements PushSender {

    @Override
    public PushResult send(List<String> tokens, PushMessage message) {
        log.info("푸시(로그만) 기기 {}대: {} — {} {}", tokens.size(), message.title(), message.body(), message.data());
        return new PushResult(tokens.size(), List.of());
    }
}
