# DB 백업 · 복원 가이드

운영 DB(Supabase PostgreSQL)는 GitHub Actions **DB 주간 백업** 워크플로가 매주 월요일 03:00(KST)에 백업합니다.

| 항목 | 내용 |
| --- | --- |
| 워크플로 | `.github/workflows/db-backup.yml` (수동 실행: Actions → DB 주간 백업 → Run workflow) |
| 범위 | `public` 스키마 전체 (테이블 · 데이터 · 인덱스 · 제약 · Flyway 이력) |
| 형식 | `pg_dump --format=custom` → GPG AES-256 암호화 (`*.dump.gpg`) + SHA-256 |
| 보관 | Actions 아티팩트 30일 (저장소 권한이 있는 사람만 내려받기 가능) |
| 검증 | 백업 직후 `pg_restore --list` 로 목차를 읽고 `member · space · space_application · flyway_schema_history` 데이터가 있는지 확인 |

> 업로드한 파일(사진 · 증빙)은 Supabase Storage 에 있어 이 백업에 들어가지 않습니다. DB 에는 저장 경로만 있습니다.

## 1. 처음 한 번 — Secrets 등록

저장소 Settings → Secrets and variables → Actions

| 이름 | 값 | 비고 |
| --- | --- | --- |
| `DB_URL` · `DB_USERNAME` · `DB_PASSWORD` | 운영과 같은 값 | 이미 등록되어 있음 (CD 와 공유) |
| `BACKUP_PASSPHRASE` | 긴 임의 문자열 (`openssl rand -base64 32`) | **팀 비밀번호 관리 도구에도 따로 보관** — 잃으면 백업을 열 수 없습니다 |
| `BACKUP_DB_URL` (선택) | `jdbc:postgresql://<세션 풀러 호스트>:5432/postgres?sslmode=require` | `DB_URL` 이 트랜잭션 풀러(포트 6543)일 때만. pg_dump 는 세션 연결이 필요합니다 |

등록 후 Actions → **DB 주간 백업 → Run workflow** 로 한 번 돌려 초록불을 확인하세요.

## 2. 복원

### 2-1. 내려받기 · 확인 · 복호화

```bash
# Actions 실행 화면 아래 Artifacts 에서 db-backup-YYYYMMDD-HHMM.zip 내려받아 풀기
sha256sum -c dasi-siheung-YYYYMMDD-HHMM.dump.gpg.sha256        # macOS: shasum -a 256 -c
gpg --decrypt --output dasi-siheung.dump dasi-siheung-YYYYMMDD-HHMM.dump.gpg   # BACKUP_PASSPHRASE 입력
pg_restore --list dasi-siheung.dump | head                       # 목차가 보이면 정상
```

### 2-2. 빈 DB 에 복원 (권장: 새 Supabase 프로젝트 또는 로컬 PostgreSQL)

```bash
# 대상 DB 연결 정보
export PGHOST=... PGPORT=5432 PGUSER=postgres PGDATABASE=postgres PGPASSWORD=...

# 1) 확장 먼저 (이미 있으면 그대로)
psql -c "CREATE EXTENSION IF NOT EXISTS postgis; CREATE EXTENSION IF NOT EXISTS btree_gist; CREATE EXTENSION IF NOT EXISTS pgcrypto;"

# 2) 복원 — 확장이 만든 객체(spatial_ref_sys 등)가 이미 있다는 오류는 무시해도 됩니다
pg_restore --no-owner --no-privileges --schema=public --dbname="$PGDATABASE" dasi-siheung.dump

# 3) 확인
psql -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;"
psql -c "SELECT (SELECT count(*) FROM member) AS members, (SELECT count(*) FROM space) AS spaces;"
```

- `pg_restore` 버전은 백업한 `pg_dump`(17) 이상이어야 합니다. (`brew install postgresql@17`)
- 복원한 DB 로 서버를 띄우면 Flyway 가 이력을 보고 **추가 마이그레이션만** 실행합니다.

### 2-3. 운영 DB 를 덮어써야 할 때

운영 데이터를 지우는 작업이므로 반드시 둘 이상이 함께 확인합니다.

1. Cloudtype 에서 서버를 **중지**해 새 쓰기를 막는다.
2. 지금 상태를 먼저 수동 백업한다. (Actions → Run workflow)
3. `public` 스키마의 테이블을 비우고(`pg_restore --clean --if-exists ...`) 위 2-2 를 진행한다.
4. 서버를 다시 시작하고 `/actuator/health`, `http/infra.http` 로 확인한다.

## 3. 복원 연습

한 달에 한 번, 최근 백업을 **새 Supabase 프로젝트(무료)** 나 로컬 PostgreSQL 에 2-2 대로 복원해 회원 · 공간 수가 운영과 비슷한지 확인합니다. 백업은 복원해 봐야 믿을 수 있습니다.
