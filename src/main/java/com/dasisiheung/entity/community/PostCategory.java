package com.dasisiheung.entity.community;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 커뮤니티 게시판 분류. 공지는 관리자만 쓴다. */
@Getter
@RequiredArgsConstructor
public enum PostCategory implements LabeledEnum {

    NOTICE("공지", true),
    LOCAL_NEWS("시흥 지역 소식", false),
    SPACE_STORY("공간 활용 이야기", false),
    RECRUIT("팀 · 협업 모집", false),
    QUESTION("질문", false);

    private final String label;
    private final boolean adminOnly;
}
