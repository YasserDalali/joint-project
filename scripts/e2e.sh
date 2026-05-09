#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ -f "$ROOT/.env" ]]; then
  # shellcheck disable=SC1091
  set -a && source "$ROOT/.env" && set +a
fi

SA_PASSWORD="${SA_PASSWORD:?set SA_PASSWORD (see .env.example)}"

if ! command -v mvn >/dev/null 2>&1; then
  echo "mvn not found on PATH (install Maven or add it to PATH)." >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq not found on PATH (required for scripts/e2e-happy-path.sh)." >&2
  exit 1
fi

echo "e2e: resetting compose stack..."
docker compose down -v
docker compose up -d sqlserver

echo "e2e: waiting for SQL Server health..."
for _ in $(seq 1 90); do
  if docker compose exec -T sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$SA_PASSWORD" -Q "SELECT 1" -b -o /dev/null >/dev/null 2>&1; then
    echo "e2e: sqlserver is up"
    break
  fi
  sleep 2
done

echo "e2e: applying migrations..."
bash db/scripts/apply-migrations.sh FinRiskDB
bash db/scripts/apply-migrations.sh FinRiskDB_Test

echo "e2e: unit tests (mvn test)..."
export SA_PASSWORD
mvn -B test

echo "e2e: integration tests (mvn verify)..."
mvn -B verify

echo "e2e: seed FinRiskDB..."
bash db/scripts/seed.sh FinRiskDB

echo "e2e: building & starting API container..."
docker compose up -d --build finrisk-api

API_BASE_URL="${API_BASE_URL:-http://localhost:18080}"

echo "e2e: waiting for HTTP health (${API_BASE_URL})..."
for _ in $(seq 1 90); do
  if curl -fsS "${API_BASE_URL}/actuator/health" >/dev/null 2>&1; then
    echo "e2e: API healthy"
    break
  fi
  sleep 2
done

curl -fsS "${API_BASE_URL}/actuator/health" >/dev/null

echo "e2e: happy-path curls..."
BASE_URL="${API_BASE_URL}" bash scripts/e2e-happy-path.sh

echo "e2e: tearing down compose..."
docker compose down

echo "e2e: ALL GREEN"
