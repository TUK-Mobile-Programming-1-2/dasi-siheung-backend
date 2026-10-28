package com.dasisiheung.service;

import com.dasisiheung.util.FileType;

/**
 * 저장소에 올린 파일 정보. DB 에는 path 만 저장하고 주소는 조회할 때 만든다.
 *
 * @param path         버킷 안의 경로 (예: spaces/1/photos/uuid.jpg)
 * @param type         판별한 파일 형식
 * @param originalName 사용자가 올린 파일 이름 (화면 표시용)
 * @param size         바이트 수
 */
public record StoredFile(String path, FileType type, String originalName, long size) {
}
