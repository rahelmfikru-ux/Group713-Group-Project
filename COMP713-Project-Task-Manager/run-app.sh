#!/bin/zsh

set -e

cd "$(dirname "$0")"

source ./.env.local

PAYARA="$HOME/.m2/repository/fish/payara/extras/payara-micro/6.2025.10/payara-micro-6.2025.10.jar"
MYSQL="$HOME/.m2/repository/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar"
WAR="$PWD/target/project-task-manager.war"

if [[ -z "$DB_PASSWORD" ]]; then
    echo "Database password is missing."
    exit 1
fi

if [[ ! -f "$PAYARA" ]]; then
    echo "Payara Micro JAR not found."
    exit 1
fi

if [[ ! -f "$MYSQL" ]]; then
    echo "MySQL Connector/J not found."
    exit 1
fi

echo "Building project..."
mvn -q clean package

echo "Starting Project & Task Manager..."
exec java -jar "$PAYARA" \
    --addLibs "$MYSQL" \
    --port 8080 \
    --deploy "$WAR"
