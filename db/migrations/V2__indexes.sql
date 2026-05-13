/*
  Indexat: lookups, joins, reporting.

  Index names match sys.indexes.name (string literal OK here).

  OBJECT_ID('dbo.accounts') : ID dyal table; kanst3mloh f object_id bach nrbtou index b table s7i7a.

  IF NOT EXISTS (...sys.indexes...) : idempotent - ma ncreatiwch nefs index marratayn (safe rerun / CI).

  DESC f kolonn dyal date : kay3awn queries li katleb akher transactions lwel (recent-first).

  Ma nCREATE INDEX direct bla check : migration momkin ttskl ila index deja kayn.
*/

-- Kan-zido l index ghir ila ma kaynch (rerun safe).
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_accounts_user_id' AND object_id = OBJECT_ID('dbo.accounts'))
    CREATE NONCLUSTERED INDEX IX_accounts_user_id ON dbo.accounts (user_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_transactions_account_id' AND object_id = OBJECT_ID('dbo.transactions'))
    CREATE NONCLUSTERED INDEX IX_transactions_account_id ON dbo.transactions (account_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_transactions_asset_id' AND object_id = OBJECT_ID('dbo.transactions'))
    CREATE NONCLUSTERED INDEX IX_transactions_asset_id ON dbo.transactions (asset_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_transactions_account_date' AND object_id = OBJECT_ID('dbo.transactions'))
    CREATE NONCLUSTERED INDEX IX_transactions_account_date ON dbo.transactions (account_id, transaction_date DESC);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_assets_symbol' AND object_id = OBJECT_ID('dbo.assets'))
    CREATE NONCLUSTERED INDEX IX_assets_symbol ON dbo.assets (symbol);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_assets_asset_type' AND object_id = OBJECT_ID('dbo.assets'))
    CREATE NONCLUSTERED INDEX IX_assets_asset_type ON dbo.assets (asset_type);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_price_history_asset_recorded' AND object_id = OBJECT_ID('dbo.asset_price_history'))
    CREATE NONCLUSTERED INDEX IX_price_history_asset_recorded ON dbo.asset_price_history (asset_id, recorded_at DESC);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_audit_logs_entity' AND object_id = OBJECT_ID('dbo.audit_logs'))
    CREATE NONCLUSTERED INDEX IX_audit_logs_entity ON dbo.audit_logs (entity_name, entity_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_users_email' AND object_id = OBJECT_ID('dbo.users'))
    CREATE NONCLUSTERED INDEX IX_users_email ON dbo.users (email);
GO
