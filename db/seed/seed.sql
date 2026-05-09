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

INSERT INTO dbo.users (full_name, email, created_at)
VALUES (N'Alice Investor', N'alice@finrisk.local', SYSUTCDATETIME());

DECLARE @uid BIGINT = SCOPE_IDENTITY();

INSERT INTO dbo.accounts (user_id, account_name, cash_balance, created_at)
VALUES (@uid, N'Primary', 50000.0000, SYSUTCDATETIME());

INSERT INTO dbo.assets (symbol, name, asset_type, current_price, risk_level, created_at)
VALUES (N'AAPL', N'Apple Inc.', N'STOCK', 180.0000, N'HIGH', SYSUTCDATETIME());
DECLARE @stockId BIGINT = SCOPE_IDENTITY();
INSERT INTO dbo.asset_details_stock (asset_id, sector, exchange_name)
VALUES (@stockId, N'Technology', N'NASDAQ');

INSERT INTO dbo.assets (symbol, name, asset_type, current_price, risk_level, created_at)
VALUES (N'SPY', N'SPDR S&P 500 ETF', N'ETF', 450.0000, N'MEDIUM', SYSUTCDATETIME());
DECLARE @etfId BIGINT = SCOPE_IDENTITY();
INSERT INTO dbo.asset_details_etf (asset_id, issuer, expense_ratio)
VALUES (@etfId, N'State Street', 0.094500);

INSERT INTO dbo.assets (symbol, name, asset_type, current_price, risk_level, created_at)
VALUES (N'US10Y', N'US Treasury 10Y', N'BOND', 98.5000, N'LOW', SYSUTCDATETIME());
DECLARE @bondId BIGINT = SCOPE_IDENTITY();
INSERT INTO dbo.asset_details_bond (asset_id, interest_rate, maturity_date, issuer)
VALUES (@bondId, 4.2500, DATEFROMPARTS(2034, 5, 15), N'US Treasury');

INSERT INTO dbo.assets (symbol, name, asset_type, current_price, risk_level, created_at)
VALUES (N'BTC', N'Bitcoin', N'CRYPTO', 62000.0000, N'VERY_HIGH', SYSUTCDATETIME());
DECLARE @cryptoId BIGINT = SCOPE_IDENTITY();
INSERT INTO dbo.asset_details_crypto (asset_id, blockchain)
VALUES (@cryptoId, N'Bitcoin');

DECLARE @d INT = 0;
WHILE @d < 30
BEGIN
    INSERT INTO dbo.asset_price_history (asset_id, price, recorded_at)
    VALUES (@stockId, CAST(170.0000 AS DECIMAL(19,4)) + @d * CAST(0.1000 AS DECIMAL(19,4)),
           DATEADD(DAY, -@d, SYSUTCDATETIME()));
    INSERT INTO dbo.asset_price_history (asset_id, price, recorded_at)
    VALUES (@etfId, CAST(440.0000 AS DECIMAL(19,4)) + @d * CAST(0.0500 AS DECIMAL(19,4)),
           DATEADD(DAY, -@d, SYSUTCDATETIME()));
    INSERT INTO dbo.asset_price_history (asset_id, price, recorded_at)
    VALUES (@bondId, CAST(98.0000 AS DECIMAL(19,4)) + @d * CAST(0.0100 AS DECIMAL(19,4)),
           DATEADD(DAY, -@d, SYSUTCDATETIME()));
    INSERT INTO dbo.asset_price_history (asset_id, price, recorded_at)
    VALUES (@cryptoId, CAST(60000.0000 AS DECIMAL(19,4)) + @d * CAST(10.0000 AS DECIMAL(19,4)),
           DATEADD(DAY, -@d, SYSUTCDATETIME()));
    SET @d = @d + 1;
END
