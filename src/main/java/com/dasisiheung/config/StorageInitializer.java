package com.dasisiheung.config;

import com.dasisiheung.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 서버가 요청을 받을 준비가 되면 저장소를 준비한다. (Supabase: 버킷이 없으면 생성) */
@Component
@RequiredArgsConstructor
public class StorageInitializer {

    private final FileStorage fileStorage;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        fileStorage.initialize();
    }
}
