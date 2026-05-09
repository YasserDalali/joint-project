/* Supporting indexes for lookups, joins, and reporting */

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_accounts_user_id' AND object_id = OBJECT_ID(N'dbo.accounts'))
    CREATE NONCLUSTERED INDEX IX_accounts_user_id ON dbo.accounts (user_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_transactions_account_id' AND object_id = OBJECT_ID(N'dbo.transactions'))
    CREATE NONCLUSTERED INDEX IX_transactions_account_id ON dbo.transactions (account_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_transactions_asset_id' AND object_id = OBJECT_ID(N'dbo.transactions'))
    CREATE NONCLUSTERED INDEX IX_transactions_asset_id ON dbo.transactions (asset_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_transactions_account_date' AND object_id = OBJECT_ID(N'dbo.transactions'))
    CREATE NONCLUSTERED INDEX IX_transactions_account_date ON dbo.transactions (account_id, transaction_date DESC);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_assets_symbol' AND object_id = OBJECT_ID(N'dbo.assets'))
    CREATE NONCLUSTERED INDEX IX_assets_symbol ON dbo.assets (symbol);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_assets_asset_type' AND object_id = OBJECT_ID(N'dbo.assets'))
    CREATE NONCLUSTERED INDEX IX_assets_asset_type ON dbo.assets (asset_type);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_price_history_asset_recorded' AND object_id = OBJECT_ID(N'dbo.asset_price_history'))
    CREATE NONCLUSTERED INDEX IX_price_history_asset_recorded ON dbo.asset_price_history (asset_id, recorded_at DESC);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_audit_logs_entity' AND object_id = OBJECT_ID(N'dbo.audit_logs'))
    CREATE NONCLUSTERED INDEX IX_audit_logs_entity ON dbo.audit_logs (entity_name, entity_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_users_email' AND object_id = OBJECT_ID(N'dbo.users'))
    CREATE NONCLUSTERED INDEX IX_users_email ON dbo.users (email);
GO
