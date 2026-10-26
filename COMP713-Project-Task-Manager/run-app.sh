#!/usr/bin/env bash
#
# Builds the API + website and starts it on http://localhost:8080/project-task-manager/
# (macOS / Linux). Windows: use run-app.ps1.
#
# The H2 database is a local file (data/), created automatically on
# first start, so no database server or password is needed.

set -e

cd "$(dirname "$0")"

PAYARA_VERSION="6.2025.10"
PAYARA="payara-micro-$PAYARA_VERSION.jar"

if [[ ! -f "$PAYARA" ]]; then
    echo "Downloading Payara Micro $PAYARA_VERSION (first run only)..."
    mvn -q dependency:copy \
        "-Dartifact=fish.payara.extras:payara-micro:$PAYARA_VERSION" \
        "-DoutputDirectory=."
fi

echo "Building project..."
mvn -q clean package

echo "Starting Project & Task Manager on port 8080..."
exec java -jar "$PAYARA" \
    --port 8080 \
    --deploy target/project-task-manager.war