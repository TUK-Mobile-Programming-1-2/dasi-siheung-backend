## 작업 내용

<!-- 무엇을, 왜 바꿨는지 -->

## 관련 이슈

<!-- 예: #12 -->

## API 변경

- [ ] 변경 없음
- [ ] 새 API 추가 / 기존 API 변경 → 아래 확인

## 확인

- [ ] `./gradlew build` 통과
- [ ] 새 API 에 REST Docs 테스트 작성
- [ ] `./gradlew generateApiDocs` 로 `openapi3.yaml` 갱신 후 함께 커밋
- [ ] `http/` 에 기대값이 담긴 `.http` 요청 추가
- [ ] DB 변경이 있으면 Flyway 마이그레이션(`V{번호}__설명.sql`) 추가
