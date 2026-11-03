#!/usr/bin/env bash
# 시연 리허설: http/demo/demo-scenario.http 를 N번(기본 10) 반복 실행해 매번 통과하는지 확인한다.
#
#   bash scripts/demo-rehearsal.sh            # local 서버에 10번
#   ENV=prod bash scripts/demo-rehearsal.sh 3 # 다른 환경에 3번
#
# 준비: demo 프로필로 띄운 서버 (docs/DEMO_GUIDE.md), ijhttp(IntelliJ HTTP Client CLI), http/http-client.private.env.json
# 회차마다 신청 날짜를 이틀씩 뒤로 옮겨(runIndex) 같은 시연 데이터로 여러 번 돌릴 수 있다.
set -uo pipefail

RUNS="${1:-10}"
ENV_NAME="${ENV:-local}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$ROOT/build/demo-rehearsal"

cd "$ROOT/http"
if ! command -v ijhttp > /dev/null; then
  echo "ijhttp 가 필요합니다 → https://www.jetbrains.com/help/idea/http-client-cli.html"
  exit 1
fi
if [ ! -f http-client.private.env.json ]; then
  echo "http/http-client.private.env.json (관리자 계정)이 필요합니다 → docs/ADMIN_GUIDE.md"
  exit 1
fi
if [ "$RUNS" -gt 15 ]; then
  echo "회차마다 신청 날짜가 이틀씩 밀려 시연 캘린더(60일)를 넘습니다. 15번 이하로 실행하세요."
  exit 1
fi

mkdir -p "$LOG_DIR"
passed=0
failed_runs=()
total_start=$(date +%s)
for i in $(seq 1 "$RUNS"); do
  start=$(date +%s)
  log="$LOG_DIR/run-$i.log"
  if ijhttp --env-file http-client.env.json --private-env-file http-client.private.env.json --env "$ENV_NAME" \
      -V "runIndex=$i" demo/demo-scenario.http > "$log" 2>&1; then
    passed=$((passed + 1))
    printf '✔ %2d회차 통과 (%ss)\n' "$i" "$(( $(date +%s) - start ))"
  else
    failed_runs+=("$i")
    printf '✖ %2d회차 실패 (%ss) → %s\n' "$i" "$(( $(date +%s) - start ))" "$log"
  fi
done

echo "────────────────────────────"
echo "통과 $passed / $RUNS  (총 $(( $(date +%s) - total_start ))s, 로그: build/demo-rehearsal/)"
if [ "${#failed_runs[@]}" -gt 0 ]; then
  echo "실패 회차: ${failed_runs[*]}"
  exit 1
fi
