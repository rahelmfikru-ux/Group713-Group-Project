#!/usr/bin/env bash
#
# Non-functional requirement: response time
#
# Measures how long the REST API takes for the two calls the
# Android employee app makes most often:
#   1. GET /api/users/{id}/tasks              (load my tasks)
#   2. PUT /api/projects/{p}/tasks/{t}/status (change status)
#
# Acceptance target: 95th percentile (p95) <= TARGET_MS for both.
#
# Usage (server must already be running, e.g. ./run-app.sh):
#   ./tests/response-time-test.sh
#   BASE_URL=http://localhost:8080/project-task-manager/api RUNS=100 ./tests/response-time-test.sh
#
# Works with bash 3.2 (macOS default) and Git Bash on Windows.
# Only needs curl, sort and awk.

set -u

BASE_URL="${BASE_URL:-http://localhost:8080/project-task-manager/api}"
RUNS="${RUNS:-50}"
WARMUP="${WARMUP:-5}"
TARGET_MS="${TARGET_MS:-500}"

RESULTS_DIR="$(cd "$(dirname "$0")" && pwd)/results"
mkdir -p "$RESULTS_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
CSV="$RESULTS_DIR/response-time-$STAMP.csv"

# ---------- helpers ----------

# Pull the first numeric "id" out of a JSON response.
json_id() {
    sed -n 's/.*"id"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' | head -1
}

post_json() {
    curl -sS -X POST "$BASE_URL$1" \
        -H "Content-Type: application/json" \
        -d "$2"
}

# Prints "<http_code> <milliseconds>" for one request.
timed_request() {
    method="$1"; path="$2"; body="${3:-}"

    if [ -n "$body" ]; then
        out=$(curl -sS -o /dev/null -w "%{http_code} %{time_total}" \
            -X "$method" "$BASE_URL$path" \
            -H "Content-Type: application/json" -d "$body")
    else
        out=$(curl -sS -o /dev/null -w "%{http_code} %{time_total}" \
            -X "$method" "$BASE_URL$path")
    fi

    code=$(echo "$out" | awk '{print $1}')
    ms=$(echo "$out" | awk '{printf "%.1f", $2 * 1000}')
    echo "$code $ms"
}

# Reads milliseconds (one per line) and prints min/median/p95/max/mean.
summarise() {
    sort -n | awk '
        { v[NR] = $1; sum += $1 }
        END {
            if (NR == 0) { print "none"; exit }
            p50 = v[int((NR - 1) * 0.50) + 1]
            p95 = v[int((NR - 1) * 0.95) + 1]
            printf "%.1f %.1f %.1f %.1f %.1f\n", v[1], p50, p95, v[NR], sum / NR
        }'
}

# ---------- 1. check the server is up ----------

echo "Response-time test against $BASE_URL"
echo "Runs per operation: $RUNS (plus $WARMUP warm-up), target p95 <= ${TARGET_MS} ms"
echo

if ! curl -sS -o /dev/null -f "$BASE_URL/projects"; then
    echo "FAILED: cannot reach $BASE_URL/projects - is the server running?"
    exit 2
fi

# ---------- 2. create test data through the API ----------

SUFFIX="$(date +%s)"

PROJECT_ID=$(post_json "/projects" \
    "{\"name\":\"NFR test $SUFFIX\",\"description\":\"Created by response-time-test.sh\"}" | json_id)

EMPLOYEE_ID=$(post_json "/users" \
    "{\"username\":\"nfr_$SUFFIX\",\"displayName\":\"NFR Test Employee\",\"role\":\"EMPLOYEE\"}" | json_id)

if [ -z "$PROJECT_ID" ] || [ -z "$EMPLOYEE_ID" ]; then
    echo "FAILED: could not create the test project or employee."
    exit 2
fi

# Give the employee a realistic number of tasks to load.
TASK_ID=""
for i in 1 2 3 4 5 6 7 8 9 10; do
    id=$(post_json "/projects/$PROJECT_ID/tasks" \
        "{\"title\":\"NFR task $i\",\"assignedUserId\":$EMPLOYEE_ID}" | json_id)
    [ -z "$TASK_ID" ] && TASK_ID="$id"
done

if [ -z "$TASK_ID" ]; then
    echo "FAILED: could not create test tasks."
    exit 2
fi

echo "Test data: project $PROJECT_ID, employee $EMPLOYEE_ID, 10 assigned tasks"
echo

echo "operation,run,http_code,ms" > "$CSV"

# ---------- 3. measure ----------

STATUSES="TODO IN_PROGRESS DONE"

measure() {
    label="$1"; method="$2"; path="$3"
    total=$((WARMUP + RUNS))
    n=0
    while [ "$n" -lt "$total" ]; do
        n=$((n + 1))

        body=""
        if [ "$method" = "PUT" ]; then
            # Cycle through the statuses so every update is a real change.
            s=$(echo "$STATUSES" | awk -v i="$n" '{print $((i % 3) + 1)}')
            body="{\"status\":\"$s\"}"
        fi

        set -- $(timed_request "$method" "$path" "$body")
        code="$1"; ms="$2"

        # Warm-up runs are not recorded.
        if [ "$n" -gt "$WARMUP" ]; then
            echo "$label,$((n - WARMUP)),$code,$ms" >> "$CSV"
        fi
    done
}

measure "load_employee_tasks" GET "/users/$EMPLOYEE_ID/tasks"
measure "update_task_status"  PUT "/projects/$PROJECT_ID/tasks/$TASK_ID/status"

# ---------- 4. report ----------

OVERALL=0

printf "%-22s %6s %8s %8s %8s %8s %8s %8s  %s\n" \
    "operation" "ok" "min" "median" "p95" "max" "mean" "target" "result"

for op in load_employee_tasks update_task_status; do
    ok=$(awk -F, -v op="$op" '$1 == op && $3 == 200' "$CSV" | wc -l | tr -d ' ')
    errors=$(awk -F, -v op="$op" '$1 == op && $3 != 200' "$CSV" | wc -l | tr -d ' ')

    set -- $(awk -F, -v op="$op" '$1 == op && $3 == 200 { print $4 }' "$CSV" | summarise)

    if [ "$1" = "none" ]; then
        printf "%-22s %6s  no successful requests  FAIL\n" "$op" "0/$RUNS"
        OVERALL=1
        continue
    fi

    min="$1"; p50="$2"; p95="$3"; max="$4"; mean="$5"

    result="PASS"
    if [ "$errors" -gt 0 ] || awk -v p="$p95" -v t="$TARGET_MS" 'BEGIN { exit !(p > t) }'; then
        result="FAIL"
        OVERALL=1
    fi

    printf "%-22s %6s %8s %8s %8s %8s %8s %8s  %s\n" \
        "$op" "$ok/$RUNS" "$min" "$p50" "$p95" "$max" "$mean" "$TARGET_MS" "$result"
done

echo
echo "Times in milliseconds. Raw results: $CSV"
echo "Note: a test project and employee were created; they stay in the database."

exit "$OVERALL"