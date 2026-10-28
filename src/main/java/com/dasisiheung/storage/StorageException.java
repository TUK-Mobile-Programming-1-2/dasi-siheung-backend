package com.dasisiheung.storage;

/** 외부 저장소(Supabase Storage 등)와 통신하지 못했을 때. 서비스 계층이 STORAGE_UNAVAILABLE 로 바꾼다. */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(String message) {
        super(message);
    }
}
