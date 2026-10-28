package com.dasisiheung.storage;

import java.time.Duration;

/**
 * 파일 저장소. 운영은 Supabase Storage, 로컬·CI 는 로컬 디스크를 쓴다. (app.storage.provider)
 * <p>
 * path 는 버킷 안의 경로(예: spaces/1/uuid.jpg)이며 DB 에는 이 값만 저장한다.
 * 주소(URL)는 조회할 때마다 만든다. 비공개 파일의 임시 주소는 시간이 지나면 만료되기 때문이다.
 */
public interface FileStorage {

    /** @throws StorageException 저장소와 통신하지 못한 경우 */
    void upload(StorageBucket bucket, String path, byte[] content, String contentType);

    /** 공개 버킷 파일의 고정 주소. */
    String publicUrl(String path);

    /**
     * 비공개 버킷 파일의 임시 주소.
     *
     * @throws StorageException 저장소와 통신하지 못한 경우
     */
    String signedUrl(String path, Duration ttl);

    /** 없는 파일이어도 오류로 보지 않는다. @throws StorageException 저장소와 통신하지 못한 경우 */
    void delete(StorageBucket bucket, String path);

    /** 서버 기동 직후 한 번 호출된다. (버킷 준비 등) 실패해도 서버 기동은 막지 않는다. */
    default void initialize() {
    }
}
