#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${1:?usage: seed.sh <database_name>}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  # shellcheck disable=SC1091
  set -a && source "${ROOT_DIR}/.env" && set +a
fi

SA_PASSWORD="${SA_PASSWORD:?SA_PASSWORD is required}"
CONTAINER="${CONTAINER:-finrisk-sqlserver}"

docker exec -i "${CONTAINER}" /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "${SA_PASSWORD}" -d "${DB_NAME}" -b \
  < "${ROOT_DIR}/db/seed/seed.sql"

echo "Seed applied to ${DB_NAME}."
