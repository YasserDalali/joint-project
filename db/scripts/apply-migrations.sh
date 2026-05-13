#!/usr/bin/env bash
set -euo pipefail

TARGET_DB="${1:?usage: apply-migrations.sh <database_name>}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONTAINER="${CONTAINER:-finrisk-sqlserver}"
MIG_DIR="${ROOT_DIR}/db/migrations"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  # shellcheck disable=SC1091
  set -a && source "${ROOT_DIR}/.env" && set +a
fi

# `.env` sets DB_NAME for Spring/Compose — CLI target database must win
DB_NAME="${TARGET_DB}"

SA_PASSWORD="${SA_PASSWORD:?SA_PASSWORD is required}"

SQLCMD_BASE=(docker exec -i "${CONTAINER}" /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "${SA_PASSWORD}")

echo "Ensuring database ${DB_NAME} exists..."
"${SQLCMD_BASE[@]}" -b -Q "IF DB_ID('${DB_NAME}') IS NULL CREATE DATABASE [${DB_NAME}];"

run_file() {
  local f="$1"
  echo "Applying $(basename "$f")..."
  # sqlcmd runs inside the container — stream host file via stdin
  docker exec -i "${CONTAINER}" /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "${SA_PASSWORD}" -d "${DB_NAME}" -b < "$f"
}

for f in "${MIG_DIR}"/V*.sql; do
  [[ -f "$f" ]] || continue
  run_file "$f"
done

echo "Migrations applied to ${DB_NAME}."
