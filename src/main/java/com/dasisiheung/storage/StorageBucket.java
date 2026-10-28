package com.dasisiheung.storage;

/**
 * 파일 저장 위치.
 * <ul>
 *   <li>PUBLIC: 누구나 주소로 볼 수 있음 (공간 사진)</li>
 *   <li>PRIVATE: 서버가 발급한 임시 주소로만 볼 수 있음 (권한 증빙, 활동 기록 사진)</li>
 * </ul>
 */
public enum StorageBucket {
    PUBLIC,
    PRIVATE
}
