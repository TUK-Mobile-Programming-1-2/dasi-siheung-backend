# 백엔드 구현 완료 보고서

작성: 2026-10-27 · 범위: 기능 목록 1~29단계 전체

## 1. 요약

| 항목 | 결과 |
| --- | --- |
| 구현 단계 | **29 / 29** (기능 24 · 운영 3 · 시연 2) |
| API | 78개 엔드포인트, 오류 코드 72개 (기능별 표: 연동 가이드 3-3) |
| DB | Flyway 마이그레이션 V1 ~ V16 |
| 코드 | main 219 파일 · test 86 파일, `.http` 시나리오 15개 + 시연 1개 |
| 커밋 계획 | 29개 브랜치 · 165개 커밋 (`commit-features.sh`) |
| 검증 | 단계별 브랜치마다 **컴파일 통과** 확인, 커밋 스크립트를 임시 저장소에서 1~29단계 끝까지 재현 → 작업 폴더 = 최종본 |
| 미실행 | **테스트는 지시대로 실행하지 않음** → 아래 3-2 로 직접 실행 필요 |

## 2. 단계별 구현 내용

| 단계 | 브랜치 | 내용 | DB |
| --- | --- | --- | --- |
| 1 | `feat/auth-jwt` | 역할 기반 가입 · 로그인, JWT(30분) · 리프레시 토큰(14일, 재발급마다 교체), 로그아웃 | V2 · V3 |
| 2 | `feat/member-me` | 내 정보 조회 · 수정 · 비밀번호 변경 · 탈퇴(즉시 비식별화) | |
| 3 | `docs/admin-promotion` | 관리자 승격 절차 (DB) | |
| 4 | `feat/common-paging-options` | 공통 페이지 형식, 선택지 API(`/options`) | |
| 5 | `feat/file-storage` | 파일 저장소 (Supabase Storage / 로컬), 파일 시그니처 검사 | |
| 6 | `feat/space-register` | 공간 등록, 좌표로 지역 판정(PostGIS), 가능 날짜 | V4 |
| 7 | `feat/space-photos-proofs` | 사진(최대 10, 순서) · 권한 증빙(비공개, 임시 주소) | V5 |
| 8 | `feat/space-edit` | 수정(중요 항목 바뀌면 재검수) · 일시 중단 · 재검수 요청 · 내 공간 목록 | |
| 9 | `feat/admin-space-review` | 관리자 검수 (승인 / 수정 요청 / 보류, 5개 항목, 이력, 공식 정보 링크) | V6 |
| 10 | `feat/admin-dashboard` | 관리자 대시보드 | |
| 11 | `feat/space-detail` | 이용자 공간 상세 (공개 공간만) | |
| 12 | `feat/space-search` | 검색 · 필터 (지역 · 활동 · 목적 · 기간 · 예산 · 시설 · 검색어 · 정렬) | |
| 13 | `feat/space-map` | 지도 영역 마커 (GiST `&&`, 최대 300) | |
| 14 | `feat/application-submit` | 이용 신청 · 내 신청 · 상세 · 취소 | V7 |
| 15 | `feat/price-negotiation` | 가격 제안 · 역제안 · 수락 | V8 |
| 16 | `feat/provider-applications` | 받은 신청 목록 · 거절 | |
| 17 | `feat/application-approval` | **승인 = 확정**: 공간 행 잠금 + DB 배타 제약, 겹치는 신청 자동 마감, 동시 승인 테스트 | V9 |
| 18 | `feat/favorite` | 찜 (원자적 카운트, 인기순) | V10 |
| 19 | `feat/usage-review` | 이용 후기 · 평점(DB 생성 컬럼) · 답글, 평점순 | V11 |
| 20 | `feat/activity-photos` | 활동 전후 사진 (원상 복구 확인) | V12 |
| 21 | `feat/community` | 커뮤니티 5개 분류 · 댓글 (공지는 관리자) | V13 |
| 22 | `feat/conversation` | 공간별 1:1 대화 (폴링, 안 읽은 수) | V14 |
| 23 | `feat/report-moderation` | 신고 · 숨김 · 이용 정지 | V15 |
| 24 | `feat/theme-recommendation` | 상권 테마 보정, 설명 가능한 추천 | |
| 25 | `ci/db-backup` | DB 주간 백업 (pg_dump 17 → GPG → 아티팩트 30일) | |
| 26 | `feat/privacy-retention` | 개인정보 자동 파기 · 비식별화, 월별 통계 보존 | V16 |
| 27 | `feat/demo-data` | demo 프로필 시연 데이터 (관리자 미생성) | |
| 28 | `test/demo-scenario` | 시연 시나리오 `.http` + 10회 리허설 스크립트 | |
| 29 | `docs/final` | README · 연동 가이드 · 관리자 가이드 정리, 이 보고서 | |

### 설계에서 신경 쓴 부분

- **이중 예약 방지**: 같은 공간 승인은 `SELECT ... FOR UPDATE` 로 한 번에 하나씩, 잠금을 우회해도 `EXCLUDE USING gist (space_id =, daterange &&) WHERE status = 'APPROVED'` 가 막음. 위반(23P01)은 `PERIOD_ALREADY_BOOKED` 로 응답.
- **집계 컬럼 동시성**: 찜 수 · 후기 수 · 댓글 수 · 조회 수는 `UPDATE ... SET x = x + 1` 한 문장으로만 바꾸고, 엔티티에서는 읽기 전용 매핑 → 공간 수정이 옛 값을 덮어쓰지 않음. 1:1 대화는 `@DynamicUpdate` 로 양쪽 읽음 위치 보호.
- **계층형 구조 유지**: ArchUnit 규칙(계층 방향 · 엔티티 독립 · 패키지 순환 금지)을 지키도록 엔티티는 서비스 타입 대신 값만 받음 (`StoredPhotoFile` 등).
- **문서 = 테스트**: 모든 새 API 에 REST Docs 테스트(요청 · 응답 필드 검증)와 기대값 `.http` 를 함께 작성. 오류 코드는 연동 가이드 표에 기능별로 정리.

## 3. 해야 할 일 (순서대로)

### 3-1. GitHub Secrets 등록 (Settings → Secrets and variables → Actions)

| Secret | 필요 단계 | 값 |
| --- | --- | --- |
| `JWT_SECRET` | 1 | `openssl rand -base64 48` |
| `SUPABASE_URL` · `SUPABASE_SERVICE_KEY` | 5 | 운영 Supabase 주소 · secret 키 |
| `BACKUP_PASSPHRASE` | 25 | `openssl rand -base64 32` — 따로 보관 |

없으면 CD 는 배포를 건너뛰고 경고만 남깁니다.

### 3-2. 테스트 실행 (첫 단계 커밋 전에 한 번)

```bash
export TEST_DB_URL=jdbc:postgresql://<개발 DB>/postgres TEST_DB_USERNAME=... TEST_DB_PASSWORD=...
./gradlew test          # 실패하면 해당 단계 파일을 고친 뒤 진행
```

- 개발 DB 에 `btree_gist` 확장이 필요합니다 (V9 가 만듭니다. Supabase 는 기본 제공).
- `DemoDataIntegrationTest` 는 테스트 DB 에 시연 계정 7개를 남깁니다.

### 3-3. 커밋 · PR (main 에 직접 커밋하지 않음)

```bash
bash commit-features.sh list        # 단계 목록
bash commit-features.sh 1           # main 최신화 → feat/auth-jwt 생성 → 12개 커밋 → 푸시 → PR 주소 출력
# PR 생성 → CI 통과 → Rebase and merge → 다음 단계
bash commit-features.sh 2
...
bash commit-features.sh 29
```

- 이전 단계가 main 에 합쳐지지 않았으면 스크립트가 멈춥니다. (`FORCE=1` 로 무시 가능)
- 29단계까지 끝나면 `_commit/` 폴더와 `commit-features.sh` 는 지워도 됩니다. (커밋되지 않음)

### 3-4. 관리자 · `.http` 준비

1. 운영 DB 에서 관리자 승격 → [ADMIN_GUIDE](ADMIN_GUIDE.md)
2. `http/http-client.private.env.json` 에 `adminEmail` · `adminPassword` (커밋 제외 파일)

### 3-5. 검증된 명세 갱신

```bash
./gradlew generateApiDocs    # 테스트 → src/main/resources/static/docs/openapi3.yaml
```

29단계 뒤 별도 브랜치(예: `docs/openapi-spec`)로 커밋하면 프론트엔드가 `/docs/openapi3.yaml` 로 전체 명세를 받습니다.

## 4. 알려진 제약 · 다음 과제

| 항목 | 현재 | 개선 방향 |
| --- | --- | --- |
| 이용 정지 즉시성 | 리프레시 토큰은 바로 삭제, 이미 받은 액세스 토큰은 최대 30분 유효 | 필터에서 회원 상태 캐시 확인 |
| 숨김 공간 되돌리기 | API 없음 (DB 에서 상태 변경 후 재검수) | 관리자 "숨김 해제" API |
| 1:1 소통 · 알림 | 폴링 (5초 · 30초) + FCM 푸시: 새 메시지 · 새 이용 신청 (2026-11) | 가격 제안 · 승인 · 거절 푸시 추가 |
| 결제 | 앱 밖에서 진행, 서버는 합의 금액만 기록 | PG 연동은 범위 밖 |
| 시연 데이터 사진 | 없음 (대표 사진 null) | 시연 전 제공자 계정으로 사진 업로드 |
| 테스트 | 컴파일만 확인 | 3-2 실행 후 실패 항목 수정 |
