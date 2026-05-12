SET NOCOUNT ON;

DELETE FROM dbo.audit_logs;
DELETE FROM dbo.transactions;
DELETE FROM dbo.asset_price_history;
DELETE FROM dbo.asset_details_crypto;
DELETE FROM dbo.asset_details_bond;
DELETE FROM dbo.asset_details_etf;
DELETE FROM dbo.asset_details_stock;
DELETE FROM dbo.accounts;
DELETE FROM dbo.assets;
DELETE FROM dbo.users;

DBCC CHECKIDENT ('dbo.users', RESEED, 0);
DBCC CHECKIDENT ('dbo.accounts', RESEED, 0);
DBCC CHECKIDENT ('dbo.assets', RESEED, 0);
DBCC CHECKIDENT ('dbo.transactions', RESEED, 0);
DBCC CHECKIDENT ('dbo.asset_price_history', RESEED, 0);
DBCC CHECKIDENT ('dbo.audit_logs', RESEED, 0);

/* ------------------------------------------------------------------------- */
/* Users (250) — unique emails                                              */
/* ------------------------------------------------------------------------- */
;WITH nums AS (
    SELECT TOP (250)
           ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_columns AS a
    CROSS JOIN sys.all_columns AS b
)
INSERT INTO dbo.users (full_name, email, created_at)
SELECT N'Seed User ' + CAST(n AS NVARCHAR(10)),
       N'u' + RIGHT(REPLICATE(N'0', 6) + CAST(n AS NVARCHAR(6)), 6) + N'@seed.finrisk.local',
       DATEADD(SECOND, -n, SYSUTCDATETIME())
FROM nums;

/* ------------------------------------------------------------------------- */
/* Two accounts per user (500 rows), nonnegative cash                        */
/* ------------------------------------------------------------------------- */
INSERT INTO dbo.accounts (user_id, account_name, cash_balance, created_at)
SELECT id,
       N'Primary',
       CAST(15000.0000 + ABS(CHECKSUM(NEWID())) % 185000 AS DECIMAL(19, 4)),
       SYSUTCDATETIME()
FROM dbo.users;

INSERT INTO dbo.accounts (user_id, account_name, cash_balance, created_at)
SELECT id,
       N'Secondary',
       CAST(5000.0000 + ABS(CHECKSUM(NEWID())) % 95000 AS DECIMAL(19, 4)),
       SYSUTCDATETIME()
FROM dbo.users;

/* ------------------------------------------------------------------------- */
/* Assets (150) — symbols SEED00001…SEED00150, cycling asset types           */
/* ------------------------------------------------------------------------- */
;WITH nums AS (
    SELECT TOP (150)
           ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_columns AS a
    CROSS JOIN sys.all_columns AS b
)
INSERT INTO dbo.assets (symbol, name, asset_type, current_price, risk_level, created_at)
SELECT N'SEED' + RIGHT(REPLICATE(N'0', 5) + CAST(n AS NVARCHAR(5)), 5),
       N'Seed Instrument ' + CAST(n AS NVARCHAR(10)),
       CASE ((n - 1) % 4)
           WHEN 0 THEN N'STOCK'
           WHEN 1 THEN N'ETF'
           WHEN 2 THEN N'BOND'
           ELSE N'CRYPTO'
       END,
       CAST(1.0000 + (n * 13.7) % 9000 AS DECIMAL(19, 4)),
       CASE ((n - 1) % 4)
           WHEN 0 THEN N'HIGH'
           WHEN 1 THEN N'MEDIUM'
           WHEN 2 THEN N'LOW'
           ELSE N'VERY_HIGH'
       END,
       DATEADD(MINUTE, -n, SYSUTCDATETIME())
FROM nums;

INSERT INTO dbo.asset_details_stock (asset_id, sector, exchange_name)
SELECT id,
       CASE WHEN id % 3 = 0 THEN N'Technology' WHEN id % 3 = 1 THEN N'Healthcare' ELSE N'Financials' END,
       CASE WHEN id % 2 = 0 THEN N'NASDAQ' ELSE N'NYSE' END
FROM dbo.assets
WHERE asset_type = N'STOCK';

INSERT INTO dbo.asset_details_etf (asset_id, issuer, expense_ratio)
SELECT id,
       N'Seed Issuer ' + CAST((id % 50) + 1 AS NVARCHAR(10)),
       CAST(0.01 + (id % 100) / 10000.0 AS DECIMAL(19, 6))
FROM dbo.assets
WHERE asset_type = N'ETF';

INSERT INTO dbo.asset_details_bond (asset_id, interest_rate, maturity_date, issuer)
SELECT id,
       CAST(1.5 + (id % 80) / 10.0 AS DECIMAL(9, 4)),
       DATEFROMPARTS(2030 + (id % 15), ((id % 12) + 1), 15),
       N'Seed Sovereign ' + CAST((id % 20) + 1 AS NVARCHAR(10))
FROM dbo.assets
WHERE asset_type = N'BOND';

INSERT INTO dbo.asset_details_crypto (asset_id, blockchain)
SELECT id,
       CASE WHEN id % 3 = 0 THEN N'Ethereum' WHEN id % 3 = 1 THEN N'Bitcoin' ELSE N'Solana' END
FROM dbo.assets
WHERE asset_type = N'CRYPTO';

/* ------------------------------------------------------------------------- */
/* Price history: 45 days per asset (enough for volatility / risk endpoints) */
/* ------------------------------------------------------------------------- */
;WITH days AS (
    SELECT TOP (45)
           ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 AS day_offset
    FROM sys.all_columns AS a
    CROSS JOIN sys.all_columns AS b
)
INSERT INTO dbo.asset_price_history (asset_id, price, recorded_at)
SELECT a.id,
       CAST(
           a.current_price
           * (1.0 + 0.012 * SIN(CAST(d.day_offset AS FLOAT) + CAST(a.id AS FLOAT) / 17.0))
           AS DECIMAL(19, 4)
       ),
       DATEADD(DAY, -d.day_offset, SYSUTCDATETIME())
FROM dbo.assets AS a
CROSS JOIN days AS d;

/* ------------------------------------------------------------------------- */
/* Account 1: three BUY rows on first three seed assets (SEED00001–03) so     */
/* GET /accounts/1/portfolio and /risk return non-empty data after reseed.  */
/* ------------------------------------------------------------------------- */
DECLARE @demo_account BIGINT = 1;
UPDATE dbo.accounts SET cash_balance = 5000000.0000 WHERE id = @demo_account;

DECLARE @aid1 BIGINT = 1;
DECLARE @aid2 BIGINT = 2;
DECLARE @aid3 BIGINT = 3;
DECLARE @p1 DECIMAL(19, 4) = (SELECT current_price FROM dbo.assets WHERE id = @aid1);
DECLARE @p2 DECIMAL(19, 4) = (SELECT current_price FROM dbo.assets WHERE id = @aid2);
DECLARE @p3 DECIMAL(19, 4) = (SELECT current_price FROM dbo.assets WHERE id = @aid3);

UPDATE dbo.accounts
SET cash_balance = cash_balance - (100 * @p1 + 100 * @p2 + 100 * @p3)
WHERE id = @demo_account;

INSERT INTO dbo.transactions (account_id, asset_id, transaction_type, quantity, unit_price, transaction_date)
VALUES (@demo_account, @aid1, N'BUY', 100, @p1, SYSUTCDATETIME()),
       (@demo_account, @aid2, N'BUY', 100, @p2, SYSUTCDATETIME()),
       (@demo_account, @aid3, N'BUY', 100, @p3, SYSUTCDATETIME());

GO
