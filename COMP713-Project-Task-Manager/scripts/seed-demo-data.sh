#!/usr/bin/env bash
#
# Loads demo data through the REST API: one manager, two employees,
# two projects and six tasks (some assigned, some in progress).
#
# Usage (server must already be running):
#   ./scripts/seed-demo-data.sh
#   BASE_URL=http://localhost:8080/project-task-manager/api ./scripts/seed-demo-data.sh
#
# Safe to run twice: it stops if the demo manager already exists.
# Works with bash 3.2 (macOS) and Git Bash on Windows. Needs curl.

set -u

BASE_URL="${BASE_URL:-http://localhost:8080/project-task-manager/api}"

BODY=""

post() {   # post PATH JSON -> sets BODY, exits on failure
    local tmp code
    tmp=$(mktemp)
    code=$(curl -sS -o "$tmp" -w "%{http_code}" -X POST "$BASE_URL$1" \
        -H "Content-Type: application/json" -d "$2")
    BODY=$(cat "$tmp")
    rm -f "$tmp"

    if [ "$code" != "201" ]; then
        echo "FAILED: POST $1 returned HTTP $code: $BODY"
        exit 1
    fi
}

json_id() {
    echo "$BODY" | sed -n 's/^[^{]*{[^}]*"id"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' | head -1
}

set_status() {   # set_status PROJECT_ID TASK_ID STATUS
    curl -sS -o /dev/null -X PUT "$BASE_URL/projects/$1/tasks/$2/status" \
        -H "Content-Type: application/json" -d "{\"status\":\"$3\"}"
}

# ---------- checks ----------

if ! USERS=$(curl -sS -f "$BASE_URL/users"); then
    echo "FAILED: cannot reach $BASE_URL - is the server running?"
    exit 2
fi

if echo "$USERS" | grep -q '"username":"morgan"'; then
    echo "Demo data is already loaded (user 'morgan' exists). Nothing to do."
    exit 0
fi

echo "Loading demo data into $BASE_URL"

# ---------- users ----------

post "/users" '{"username":"morgan","displayName":"Morgan Lee","role":"MANAGER"}'
MANAGER=$(json_id)

post "/users" '{"username":"alex","displayName":"Alex Chen","role":"EMPLOYEE"}'
ALEX=$(json_id)

post "/users" '{"username":"priya","displayName":"Priya Patel","role":"EMPLOYEE"}'
PRIYA=$(json_id)

# ---------- projects ----------

post "/projects" '{"name":"Office Fit-out - Queen Street","description":"Network, desks and meeting rooms for the new Queen Street office."}'
OFFICE=$(json_id)

post "/projects" '{"name":"Customer Website Refresh","description":"Update the public website and contact forms."}'
WEBSITE=$(json_id)

# ---------- tasks ----------

post "/projects/$OFFICE/tasks" "{\"title\":\"Install network switches\",\"description\":\"Level 2 comms room, two 48-port switches.\",\"assignedUserId\":$ALEX}"
T1=$(json_id)

post "/projects/$OFFICE/tasks" "{\"title\":\"Set up meeting room screens\",\"description\":\"Three rooms on level 3.\",\"assignedUserId\":$ALEX}"
T2=$(json_id)

post "/projects/$OFFICE/tasks" "{\"title\":\"Test Wi-Fi coverage\",\"description\":\"Walk every floor and record signal strength.\",\"assignedUserId\":$PRIYA}"
T3=$(json_id)

post "/projects/$OFFICE/tasks" '{"title":"Order spare cables","description":"Not assigned yet."}'

post "/projects/$WEBSITE/tasks" "{\"title\":\"Fix contact form validation\",\"description\":\"Phone numbers with spaces are rejected.\",\"assignedUserId\":$PRIYA}"
T5=$(json_id)

post "/projects/$WEBSITE/tasks" "{\"title\":\"Update team photos\",\"assignedUserId\":$ALEX}"

# A realistic mix of progress
set_status "$OFFICE" "$T1" IN_PROGRESS
set_status "$OFFICE" "$T3" DONE
set_status "$WEBSITE" "$T5" IN_PROGRESS

cat <<SUMMARY

Done.

Users
  $MANAGER  Morgan Lee   (morgan)  MANAGER
  $ALEX  Alex Chen    (alex)    EMPLOYEE  - 3 tasks
  $PRIYA  Priya Patel  (priya)   EMPLOYEE  - 2 tasks

Projects
  $OFFICE  Office Fit-out - Queen Street  (4 tasks, 1 unassigned)
  $WEBSITE  Customer Website Refresh       (2 tasks)

In the Android app, choose "Alex Chen" or "Priya Patel" under "I am".
SUMMARY