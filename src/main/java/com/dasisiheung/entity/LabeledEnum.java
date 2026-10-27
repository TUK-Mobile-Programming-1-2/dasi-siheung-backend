package com.dasisiheung.entity;

/**
 * 화면에 보여줄 한글 이름이 있는 enum. 선택지 조회 API(/api/v1/options)가 code·label 쌍으로 내려준다.
 */
public interface LabeledEnum {

    String name();

    String getLabel();
}
