#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  # shellcheck disable=SC1091
  set -a && source "${ROOT_DIR}/.env" && set +a
fi

SA_PASSWORD="${SA_PASSWORD:?SA_PASSWORD is required}"
CONTAINER="${CONTAINER:-finrisk-sqlserver}"
DB_NAME="${DB_NAME:-FinRiskDB}"

SQLCMD=(docker exec -i "${CONTAINER}" /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "${SA_PASSWORD}" -d "${DB_NAME}" -b -W)

echo "Smoke: preparing deterministic ids 1 for user/account/asset..."
"${SQLCMD[@]}" -Q "
SET NOCOUNT ON;
BEGIN TRANSACTION;
DELETE FROM dbo.audit_logs;
DELETE FROM dbo.transactions;
DELETE FROM dbo.asset_price_history;
DELETE FROM dbo.asset_details_stock WHERE asset_id = 1;
DELETE FROM dbo.accounts WHERE id = 1;
DELETE FROM dbo.assets WHERE id = 1;
DELETE FROM dbo.users WHERE id = 1;
COMMIT TRANSACTION;

DBCC CHECKIDENT ('dbo.users', RESEED, 0);
DBCC CHECKIDENT ('dbo.accounts', RESEED, 0);
DBCC CHECKIDENT ('dbo.assets', RESEED, 0);

SET IDENTITY_INSERT dbo.users ON;
INSERT INTO dbo.users (id, full_name, email, created_at)
VALUES (1, N'Smoke User', N'smoke@finrisk.local', SYSUTCDATETIME());
SET IDENTITY_INSERT dbo.users OFF;

SET IDENTITY_INSERT dbo.accounts ON;
INSERT INTO dbo.accounts (id, user_id, account_name, cash_balance, created_at)
VALUES (1, 1, N'Smoke Account', 100000.0000, SYSUTCDATETIME());
SET IDENTITY_INSERT dbo.accounts OFF;

SET IDENTITY_INSERT dbo.assets ON;
INSERT INTO dbo.assets (id, symbol, name, asset_type, current_price, risk_level, created_at)
VALUES (1, N'SMK', N'Smoke Stock', N'STOCK', 200.0000, N'HIGH', SYSUTCDATETIME());
SET IDENTITY_INSERT dbo.assets OFF;

INSERT INTO dbo.asset_details_stock (asset_id, sector, exchange_name)
VALUES (1, N'Technology', N'NYSE');
"

echo "Smoke: executing sp_buy_asset..."
"${SQLCMD[@]}" -Q "EXEC dbo.sp_buy_asset @account_id=1, @asset_id=1, @quantity=10, @unit_price=180.0;"

echo "Smoke: asserting outcomes..."
OUT="$("${SQLCMD[@]}" -Q "
SET NOCOUNT ON;
DECLARE @cash DECIMAL(19,4);
SELECT @cash = cash_balance FROM dbo.accounts WHERE id = 1;
IF @cash <> 98200.0000
BEGIN
  RAISERROR('Unexpected cash_balance after buy', 16, 1);
END

IF NOT EXISTS (
  SELECT 1 FROM dbo.transactions
  WHERE account_id = 1 AND asset_id = 1 AND transaction_type = N'BUY' AND quantity = 10
)
BEGIN
  RAISERROR('Missing BUY transaction row', 16, 1);
END

IF NOT EXISTS (
  SELECT 1 FROM dbo.audit_logs
  WHERE action_type = N'BUY_TRANSACTION_CREATED'
)
BEGIN
  RAISERROR('Missing BUY_TRANSACTION_CREATED audit row', 16, 1);
END

SELECT 'SMOKE_OK' AS status;
" -h -1)"

echo "${OUT}"
if ! echo "${OUT}" | grep -q 'SMOKE_OK'; then
  echo "Smoke assertions failed."
  exit 1
fi

echo "Smoke passed."
