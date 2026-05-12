/*
  FinRisk core schema — users, accounts, assets (+ subtype tables incl. ETF),
  transactions, price history, audit logs.
*/

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        full_name NVARCHAR(100) NOT NULL,
        email NVARCHAR(150) NOT NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_users_created_at DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT UQ_users_email UNIQUE (email)
    );
END
GO

IF OBJECT_ID(N'dbo.accounts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.accounts (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        account_name NVARCHAR(100) NOT NULL,
        cash_balance DECIMAL(19,4) NOT NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_accounts_created_at DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT FK_accounts_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
        CONSTRAINT CK_accounts_cash_nonnegative CHECK (cash_balance >= 0)
    );
END
GO

IF OBJECT_ID(N'dbo.assets', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.assets (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        symbol NVARCHAR(20) NOT NULL,
        name NVARCHAR(150) NOT NULL,
        asset_type NVARCHAR(10) NOT NULL,
        current_price DECIMAL(19,4) NOT NULL,
        risk_level NVARCHAR(20) NOT NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_assets_created_at DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT CK_assets_type CHECK (asset_type IN (N'STOCK', N'ETF', N'BOND', N'CRYPTO')),
        CONSTRAINT CK_assets_price_positive CHECK (current_price > 0),
        CONSTRAINT CK_assets_risk CHECK (risk_level IN (N'LOW', N'MEDIUM', N'HIGH', N'VERY_HIGH')),
        CONSTRAINT UQ_assets_symbol UNIQUE (symbol)
    );
END
GO

IF OBJECT_ID(N'dbo.asset_details_stock', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.asset_details_stock (
        asset_id BIGINT NOT NULL PRIMARY KEY,
        sector NVARCHAR(100) NOT NULL,
        exchange_name NVARCHAR(100) NOT NULL,
        CONSTRAINT FK_asset_details_stock_asset FOREIGN KEY (asset_id) REFERENCES dbo.assets (id) ON DELETE CASCADE
    );
END
GO

IF OBJECT_ID(N'dbo.asset_details_etf', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.asset_details_etf (
        asset_id BIGINT NOT NULL PRIMARY KEY,
        issuer NVARCHAR(150) NOT NULL,
        expense_ratio DECIMAL(19,6) NULL,
        CONSTRAINT FK_asset_details_etf_asset FOREIGN KEY (asset_id) REFERENCES dbo.assets (id) ON DELETE CASCADE
    );
END
GO

IF OBJECT_ID(N'dbo.asset_details_bond', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.asset_details_bond (
        asset_id BIGINT NOT NULL PRIMARY KEY,
        interest_rate DECIMAL(9,4) NOT NULL,
        maturity_date DATE NOT NULL,
        issuer NVARCHAR(150) NOT NULL,
        CONSTRAINT FK_asset_details_bond_asset FOREIGN KEY (asset_id) REFERENCES dbo.assets (id) ON DELETE CASCADE
    );
END
GO

IF OBJECT_ID(N'dbo.asset_details_crypto', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.asset_details_crypto (
        asset_id BIGINT NOT NULL PRIMARY KEY,
        blockchain NVARCHAR(100) NOT NULL,
        CONSTRAINT FK_asset_details_crypto_asset FOREIGN KEY (asset_id) REFERENCES dbo.assets (id) ON DELETE CASCADE
    );
END
GO

IF OBJECT_ID(N'dbo.transactions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.transactions (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        account_id BIGINT NOT NULL,
        asset_id BIGINT NOT NULL,
        transaction_type NVARCHAR(4) NOT NULL,
        quantity INT NOT NULL,
        unit_price DECIMAL(19,4) NOT NULL,
        transaction_date DATETIME2(3) NOT NULL,
        CONSTRAINT FK_transactions_account FOREIGN KEY (account_id) REFERENCES dbo.accounts (id),
        CONSTRAINT FK_transactions_asset FOREIGN KEY (asset_id) REFERENCES dbo.assets (id),
        CONSTRAINT CK_transactions_type CHECK (transaction_type IN (N'BUY', N'SELL')),
        CONSTRAINT CK_transactions_qty_positive CHECK (quantity > 0),
        CONSTRAINT CK_transactions_unit_price_positive CHECK (unit_price > 0)
    );
END
GO

IF OBJECT_ID(N'dbo.asset_price_history', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.asset_price_history (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        asset_id BIGINT NOT NULL,
        price DECIMAL(19,4) NOT NULL,
        recorded_at DATETIME2(3) NOT NULL,
        CONSTRAINT FK_price_history_asset FOREIGN KEY (asset_id) REFERENCES dbo.assets (id) ON DELETE CASCADE,
        CONSTRAINT CK_price_history_price_positive CHECK (price > 0)
    );
END
GO

IF OBJECT_ID(N'dbo.audit_logs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.audit_logs (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        entity_name NVARCHAR(100) NOT NULL,
        entity_id BIGINT NOT NULL,
        action_type NVARCHAR(100) NOT NULL,
        description NVARCHAR(500) NULL,
        created_at DATETIME2(3) NOT NULL CONSTRAINT DF_audit_logs_created DEFAULT (SYSUTCDATETIME())
    );
END
GO
