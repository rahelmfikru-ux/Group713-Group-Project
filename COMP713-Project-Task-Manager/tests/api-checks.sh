#!/usr/bin/env bash
#
# Automated API checks for the Project & Task Manager.
#
# Covers: successful operations, invalid input (400), missing
# resources (404), the manager -> employee workflow, persistence of
# a status change, location check-ins and their rules, and the
# RabbitMQ consumer's activity log.
#
# Usage (server must already be running):
#   ./tests/api-checks.sh
#   SKIP_BROKER=1 ./tests/api-checks.sh     # if RabbitMQ/consumer are not running
#   BASE_URL=http://localhost:8080/project-task-manager/api ./tests/api-checks.sh
#
# Works with bash 3.2 (macOS) and Git Bash on Windows. Needs curl.
# Each run creates its own project, employees and task, so it does
# not depend on existing data. They stay in the database afterwards.

set -u

BASE_URL="${BASE_URL:-http://localhost:8080/project-task-manager/api}"
SKIP_BROKER="${SKIP_BROKER:-0}"

RESULTS_DIR="$(cd "$(dirname "$0")" && pwd)/results"
mkdir -p "$RESULTS_DIR"
REPORT="$RESULTS_DIR/api-checks-$(date +%Y%m%d-%H%M%S).txt"

PASSED=0
FAILED=0
SKIPPED=0
NUMBER=0

CODE=""
BODY=""

# ---------- helpers ----------

# request METHOD PATH [JSON]  -> sets CODE and BODY
request() {
    local tmp
    tmp=$(mktemp)

    if [ -n "${3:-}" ]; then
        CODE=$(curl -sS -o "$tmp" -w "%{http_code}" -X "$1" "$BASE_URL$2" \
            -H "Content-Type: application/json" -d "$3")
    else
        CODE=$(curl -sS -o "$tmp" -w "%{http_code}" -X "$1" "$BASE_URL$2")
    fi

    BODY=$(cat "$tmp")
    rm -f "$tmp"
}

json_id() {
    echo "$BODY" | sed -n 's/^[^{]*{[^}]*"id"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' | head -1
}

record() {   # record RESULT NAME DETAIL
    NUMBER=$((NUMBER + 1))
    printf "%-4s %-5s %s\n" "$NUMBER." "$1" "$2" | tee -a "$REPORT"
    if [ "$1" != "PASS" ] && [ -n "${3:-}" ]; then
        printf "           %s\n" "$3" | tee -a "$REPORT"
    fi

    case "$1" in
        PASS) PASSED=$((PASSED + 1)) ;;
        SKIP) SKIPPED=$((SKIPPED + 1)) ;;
        *)    FAILED=$((FAILED + 1)) ;;
    esac
}

# expect NAME EXPECTED_CODE METHOD PATH [JSON] [TEXT_THAT_BODY_MUST_CONTAIN]
expect() {
    local name="$1" expected="$2" contains="${6:-}"

    request "$3" "$4" "${5:-}"

    if [ "$CODE" != "$expected" ]; then
        record FAIL "$name" "expected HTTP $expected, got $CODE: $BODY"
    elif [ -n "$contains" ] && ! echo "$BODY" | grep -q "$contains"; then
        record FAIL "$name" "HTTP $CODE but response did not contain '$contains': $BODY"
    else
        record PASS "$name (HTTP $CODE)"
    fi
}

# ---------- start ----------

{
    echo "API checks against $BASE_URL"
    echo "Started $(date)"
    echo
} | tee "$REPORT"

if ! curl -sS -o /dev/null -f "$BASE_URL/projects"; then
    echo "FAILED: cannot reach $BASE_URL - is the server running?" | tee -a "$REPORT"
    exit 2
fi

RUN="$(date +%s)"

# ---------- projects and users ----------

expect "Create project" 201 POST "/projects" \
    "{\"name\":\"Checks $RUN\",\"description\":\"Created by api-checks.sh\"}"
PROJECT_ID=$(json_id)

expect "Reject project with blank name" 400 POST "/projects" \
    '{"name":"   "}' "required"

expect "Create employee" 201 POST "/users" \
    "{\"username\":\"check_a_$RUN\",\"displayName\":\"Check Employee A\",\"role\":\"EMPLOYEE\"}"
EMPLOYEE_A=$(json_id)

request POST "/users" \
    "{\"username\":\"check_b_$RUN\",\"displayName\":\"Check Employee B\",\"role\":\"EMPLOYEE\"}"
EMPLOYEE_B=$(json_id)

expect "Reject user with invalid role" 400 POST "/users" \
    "{\"username\":\"check_x_$RUN\",\"displayName\":\"X\",\"role\":\"ADMIN\"}" "MANAGER or EMPLOYEE"

if [ -z "$PROJECT_ID" ] || [ -z "$EMPLOYEE_A" ] || [ -z "$EMPLOYEE_B" ]; then
    echo "Cannot continue: test project or employees were not created." | tee -a "$REPORT"
    exit 2
fi

# ---------- tasks: manager assigns, employee sees it ----------

expect "Manager creates task assigned to employee A" 201 POST "/projects/$PROJECT_ID/tasks" \
    "{\"title\":\"Inspect site $RUN\",\"assignedUserId\":$EMPLOYEE_A}"
TASK_ID=$(json_id)

LONG_TITLE=$(printf 'x%.0s' $(seq 1 151))
expect "Reject task title over 150 characters" 400 POST "/projects/$PROJECT_ID/tasks" \
    "{\"title\":\"$LONG_TITLE\"}" "150"

expect "Tasks for a missing project return 404" 404 GET "/projects/999999999/tasks"

expect "Employee A's task list shows the new assignment" 200 GET "/users/$EMPLOYEE_A/tasks" \
    "" "Inspect site $RUN"

# ---------- status updates and persistence ----------

expect "Reject invalid status value" 400 PUT "/projects/$PROJECT_ID/tasks/$TASK_ID/status" \
    '{"status":"FINISHED"}' "TODO, IN_PROGRESS or DONE"

expect "Employee updates status to DONE" 200 PUT "/projects/$PROJECT_ID/tasks/$TASK_ID/status" \
    '{"status":"done"}' '"DONE"'

expect "Status change is persisted (re-read from the API)" 200 GET "/projects/$PROJECT_ID/tasks" \
    "" '"status":"DONE"'

# ---------- location check-ins ----------

expect "Assigned employee checks in with GPS" 201 POST "/projects/$PROJECT_ID/tasks/$TASK_ID/check-ins" \
    "{\"userId\":$EMPLOYEE_A,\"source\":\"GPS\",\"latitude\":-36.8485,\"longitude\":174.7633,\"accuracyMetres\":12}" '"GPS"'

expect "Reject check-in by an employee the task is not assigned to" 400 POST "/projects/$PROJECT_ID/tasks/$TASK_ID/check-ins" \
    "{\"userId\":$EMPLOYEE_B,\"source\":\"GPS\",\"latitude\":-36.8,\"longitude\":174.7}" "not assigned"

expect "Reject manual check-in without a location" 400 POST "/projects/$PROJECT_ID/tasks/$TASK_ID/check-ins" \
    "{\"userId\":$EMPLOYEE_A,\"source\":\"MANUAL\",\"locationNote\":\"\"}" "location description"

expect "Manager sees the check-in for the project" 200 GET "/projects/$PROJECT_ID/check-ins" \
    "" "Check Employee A"

# ---------- message broker: activity log ----------

expect "Reject activity log limit outside 1-100" 400 GET "/activity?limit=0" "between 1 and 100"

if [ "$SKIP_BROKER" = "1" ]; then
    record SKIP "Consumer saved the status-change event (SKIP_BROKER=1)"
else
    # The consumer runs asynchronously; give it up to 10 seconds.
    FOUND=0
    for attempt in 1 2 3 4 5 6 7 8 9 10; do
        request GET "/activity?limit=100"
        if echo "$BODY" | grep -q "\"taskId\":$TASK_ID,"; then
            FOUND=1
            break
        fi
        sleep 1
    done

    if [ "$FOUND" = "1" ]; then
        record PASS "Consumer saved the event for task $TASK_ID to the activity log (${attempt}s)"
    else
        record FAIL "Consumer saved the event for task $TASK_ID to the activity log" \
            "not found after 10 s - are RabbitMQ and task-event-consumer running?"
    fi
fi

# ---------- summary ----------

{
    echo
    echo "Passed: $PASSED   Failed: $FAILED   Skipped: $SKIPPED   (of $NUMBER)"
    echo "Test data: project $PROJECT_ID, task $TASK_ID, employees $EMPLOYEE_A and $EMPLOYEE_B"
    echo "Report saved to $REPORT"
} | tee -a "$REPORT"

[ "$FAILED" -eq 0 ]