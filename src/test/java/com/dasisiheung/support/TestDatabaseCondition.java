package com.dasisiheung.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * 로컬에 테스트 DB 가 지정되지 않았으면 통합 테스트를 건너뛴다.
 * CI 에서는 DB 문제를 조용히 넘기지 않도록 항상 실행해 실패로 드러낸다.
 */
public class TestDatabaseCondition implements ExecutionCondition {

    private static final String TEST_DB_URL = "TEST_DB_URL";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (isRunningOnCi()) {
            return ConditionEvaluationResult.enabled("CI 환경: 통합 테스트를 항상 실행합니다.");
        }
        if (isTestDatabaseConfigured()) {
            return ConditionEvaluationResult.enabled(TEST_DB_URL + " 설정됨");
        }
        return ConditionEvaluationResult.disabled(TEST_DB_URL + " 이 없어 통합 테스트를 건너뜁니다. (CI 에서는 항상 실행)");
    }

    private static boolean isRunningOnCi() {
        return Boolean.parseBoolean(System.getenv("CI"));
    }

    private static boolean isTestDatabaseConfigured() {
        String url = System.getenv(TEST_DB_URL);
        return url != null && !url.isBlank();
    }
}
