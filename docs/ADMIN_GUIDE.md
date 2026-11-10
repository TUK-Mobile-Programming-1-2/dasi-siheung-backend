# 관리자 계정 운영 가이드 (DBA)

> 관리자(ADMIN)는 **가입 API 로 만들 수 없습니다.** 일반 회원으로 가입한 계정을 DBA(백엔드 담당)가 DB 에서 직접 승격합니다.
> 서버가 관리자 계정을 자동으로 만들지 않으므로, 관리자 권한이 생기는 경로는 이 문서의 SQL 하나뿐입니다.

## 1. 승격 절차

1. 관리자 웹 담당자가 **이용자(USER)로 회원가입** 합니다. (앱·Swagger·`http/auth.http` 어느 것이든)
2. DBA 가 Supabase 대시보드 → **SQL Editor** 에서 아래 쿼리를 실행합니다.
   - 운영: `dasi-siheung-prod` 프로젝트 / 개발: `dasi-siheung-dev` 프로젝트
3. 승격된 사람은 **로그아웃 후 다시 로그인**합니다.

```sql
-- ① 대상 확인 (반드시 1건이어야 한다)
SELECT id, email, name, role, status, created_at
FROM member
WHERE email = lower('admin-web@example.com');

-- ② 승격 — 정상(ACTIVE) 회원만 바뀐다. "UPDATE 1" 이 나와야 성공
UPDATE member
SET role = 'ADMIN', updated_at = now()
WHERE email = lower('admin-web@example.com')
  AND status = 'ACTIVE';

-- ③ 기존 로그인 무효화 — 예전 역할이 담긴 토큰으로 재발급받지 못하게 한다
DELETE FROM refresh_token
WHERE member_id = (SELECT id FROM member WHERE email = lower('admin-web@example.com'));
```

### 왜 다시 로그인해야 하나요?

액세스 토큰(JWT) 안에 **발급 당시의 역할**이 들어 있습니다. 승격 직후 가지고 있던 토큰에는 아직 `USER` 가 들어 있으므로
`/api/v1/admin/**` 를 호출하면 403 `FORBIDDEN` 입니다. ③ 으로 리프레시 토큰을 지우면 기존 로그인이 끊기고,
다시 로그인해서 받은 새 토큰부터 `ADMIN` 이 반영됩니다. (③ 을 건너뛰어도 최대 30분 뒤 액세스 토큰이 만료되면 재발급 시 반영)

## 2. 관리자 목록 · 해제

```sql
-- 현재 관리자 목록
SELECT id, email, name, status, updated_at FROM member WHERE role = 'ADMIN' ORDER BY id;

-- 관리자 해제 (일반 이용자로 되돌림) + 기존 로그인 무효화
UPDATE member SET role = 'USER', updated_at = now() WHERE email = lower('admin-web@example.com');
DELETE FROM refresh_token WHERE member_id = (SELECT id FROM member WHERE email = lower('admin-web@example.com'));
```

> 해제해도 이미 발급된 액세스 토큰은 만료(최대 30분)까지 관리자 API 를 호출할 수 있습니다.
> 즉시 막아야 하면 Cloudtype 에서 `JWT_SECRET` 을 바꿔 재배포합니다. (모든 회원이 다시 로그인)

## 3. 확인

```http
### 다시 로그인한 뒤 관리자 API 호출 → 200 이면 승격 완료
GET {{baseUrl}}/api/v1/admin/dashboard
Authorization: Bearer {{adminToken}}
```

`http/` 의 관리자 시나리오는 **비공개 환경 파일**에서 관리자 계정을 읽습니다. 저장소에 올라가지 않는 파일입니다.

```jsonc
// http/http-client.private.env.json  (.gitignore 대상)
{
  "local": { "adminEmail": "admin-web@example.com", "adminPassword": "실제 비밀번호" },
  "prod":  { "adminEmail": "admin-web@example.com", "adminPassword": "실제 비밀번호" }
}
```

CI 는 서버를 띄운 뒤 같은 방식(SQL)으로 테스트용 관리자를 만들고 이 값을 넘겨 실행합니다. (`.github/workflows/ci.yml` 의 "테스트용 관리자 준비")

## 4. 주의

- 승격 쿼리는 **이메일 조건을 꼭 확인**하고 실행합니다. `WHERE` 없이 실행하면 전원이 관리자가 됩니다.
- 운영 DB 작업 전에는 Supabase SQL Editor 상단의 프로젝트 이름(`dasi-siheung-prod`)을 한 번 더 확인합니다.
- 관리자 계정은 꼭 필요한 사람에게만 부여하고, 시범 운영이 끝나면 해제합니다.

## 5. 관리자 웹이 쓰는 기능

승격한 계정으로 로그인하면 `/api/v1/admin/**` 를 쓸 수 있습니다. 자세한 요청 · 응답은 Swagger UI 의 Admin 태그를 보세요.

| 화면 | API | `.http` |
| --- | --- | --- |
| 대시보드 | `GET /admin/dashboard` — 검수 대기 · 검토 중 신청 · 처리 대기 신고 수, 상태 · 지역 · 역할별 개수 | `admin-review.http` |
| 공간 검수 | `GET /admin/spaces`, `GET /admin/spaces/{id}`, `POST /admin/spaces/{id}/reviews`, `PUT /admin/spaces/{id}/theme` | `admin-review.http`, `space.http` |
| 신고 처리 | `GET /admin/reports`, `POST /admin/reports/{id}/resolve` (기각 · 숨김 · 정지), `POST /admin/members/{id}/suspend · unsuspend` | `report.http` |
| 개인정보 · 통계 | `GET /admin/privacy/purge-preview`, `POST /admin/privacy/purge`, `GET /admin/statistics/monthly` | `admin-privacy.http` |
| 자주 묻는 질문 | `GET /faqs` (목록, 공개), `POST /admin/faqs`, `PUT /admin/faqs/{id}`, `DELETE /admin/faqs/{id}`, `PUT /admin/faqs/order` | `faq.http` |

- 이용 정지된 회원은 로그인 · 토큰 재발급이 막힙니다. 이미 받은 액세스 토큰은 최대 30분 동안 유효합니다.
- 숨김 처리한 공간은 이용자에게 보이지 않고 제공자도 수정할 수 없습니다. 되돌릴 때는 DB 에서 상태를 바꾸고 다시 검수합니다.
- 자주 묻는 질문은 처음 9개가 들어 있습니다(V22). 순서를 바꿀 때는 목록의 **모든 ID** 를 원하는 순서대로 보냅니다. 그사이 다른 관리자가 추가 · 삭제했으면 `INVALID_FAQ_ORDER` 이니 목록을 다시 불러오세요.

