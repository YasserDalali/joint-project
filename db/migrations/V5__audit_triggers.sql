/*
  Table-level CRUD audit triggers.
  We exclude dbo.audit_logs itself to avoid recursive trigger writes.
*/

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID('dbo.tr_audit_users_crud', 'TR') IS NOT NULL
    DROP TRIGGER dbo.tr_audit_users_crud;
GO
CREATE TRIGGER dbo.tr_audit_users_crud
ON dbo.users
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
    SELECT
        'users',
        COALESCE(i.id, d.id),
        CASE
            WHEN i.id IS NOT NULL AND d.id IS NULL THEN 'CREATE'
            WHEN i.id IS NOT NULL AND d.id IS NOT NULL THEN 'UPDATE'
            WHEN i.id IS NULL AND d.id IS NOT NULL THEN 'DELETE'
        END,
        'CRUD operation captured by trigger',
        SYSUTCDATETIME()
    FROM inserted i
    FULL OUTER JOIN deleted d ON i.id = d.id;
END
GO

IF OBJECT_ID('dbo.tr_audit_accounts_crud', 'TR') IS NOT NULL
    DROP TRIGGER dbo.tr_audit_accounts_crud;
GO
CREATE TRIGGER dbo.tr_audit_accounts_crud
ON dbo.accounts
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
    SELECT
        'accounts',
        COALESCE(i.id, d.id),
        CASE
            WHEN i.id IS NOT NULL AND d.id IS NULL THEN 'CREATE'
            WHEN i.id IS NOT NULL AND d.id IS NOT NULL THEN 'UPDATE'
            WHEN i.id IS NULL AND d.id IS NOT NULL THEN 'DELETE'
        END,
        'CRUD operation captured by trigger',
        SYSUTCDATETIME()
    FROM inserted i
    FULL OUTER JOIN deleted d ON i.id = d.id;
END
GO

IF OBJECT_ID('dbo.tr_audit_assets_crud', 'TR') IS NOT NULL
    DROP TRIGGER dbo.tr_audit_assets_crud;
GO
CREATE TRIGGER dbo.tr_audit_assets_crud
ON dbo.assets
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
    SELECT
        'assets',
        COALESCE(i.id, d.id),
        CASE
            WHEN i.id IS NOT NULL AND d.id IS NULL THEN 'CREATE'
            WHEN i.id IS NOT NULL AND d.id IS NOT NULL THEN 'UPDATE'
            WHEN i.id IS NULL AND d.id IS NOT NULL THEN 'DELETE'
        END,
        'CRUD operation captured by trigger',
        SYSUTCDATETIME()
    FROM inserted i
    FULL OUTER JOIN deleted d ON i.id = d.id;
END
GO

IF OBJECT_ID('dbo.tr_audit_asset_details_stock_crud', 'TR') IS NOT NULL
    DROP TRIGGER dbo.tr_audit_asset_details_stock_crud;
GO
CREATE TRIGGER dbo.tr_audit_asset_details_stock_crud
ON dbo.asset_details_stock
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
    SELECT
        'asset_details_stock',
        COALESCE(i.asset_id, d.asset_id),
        CASE
            WHEN i.asset_id IS NOT NULL AND d.asset_id IS NULL THEN 'CREATE'
            WHEN i.asset_id IS NOT NULL AND d.asset_id IS NOT NULL THEN 'UPDATE'
            WHEN i.asset_id IS NULL AND d.asset_id IS NOT NULL THEN 'DELETE'
        END,
        'CRUD operation captured by trigger',
        SYSUTCDATETIME()
    FROM inserted i
    FULL OUTER JOIN deleted d ON i.asset_id = d.asset_id;
END
GO

IF OBJECT_ID('dbo.tr_audit_asset_details_etf_crud', 'TR') IS NOT NULL
    DROP TRIGGER dbo.tr_audit_asset_details_etf_crud;
GO
CREATE TRIGGER dbo.tr_audit_asset_details_etf_crud
ON dbo.asset_details_etf
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
    SELECT
        'asset_details_etf',
        COALESCE(i.asset_id, d.asset_id),
        CASE
            WHEN i.asset_id IS NOT NULL AND d.asset_id IS NULL THEN 'CREATE'
            WHEN i.asset_id IS NOT NULL AND d.asset_id IS NOT NULL THEN 'UPDATE'
            WHEN i.asset_id IS NULL AND d.asset_id IS NOT NULL THEN 'DELETE'
        END,
        'CRUD operation captured by trigger',
        SYSUTCDATETIME()
    FROM inserted i
    FULL OUTER JOIN deleted d ON i.asset_id = d.asset_id;
END
GO

IF OBJECT_ID('dbo.tr_audit_asset_details_bond_crud', 'TR') IS NOT NULL
    DROP TRIGGER dbo.tr_audit_asset_details_bond_crud;
GO
CREATE TRIGGER dbo.tr_audit_asset_details_bond_crud
ON dbo.asset_details_bond
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
    SELECT
        'asset_details_bond',
        COALESCE(i.asset_id, d.asset_id),
        CASE
            WHEN i.asset_id IS NOT NULL AND d.asset_id IS NULL THEN 'CREATE'
            WHEN i.asset_id IS NOT NULL AND d.asset_id IS NOT NULL THEN 'UPDATE'
            WHEN i.asset_id IS NULL AND d.asset_id IS NOT NULL THEN 'DELETE'
        END,
        'CRUD operation captured by trigger',
        SYSUTCDATETIME()
    FROM inserted i
    FULL OUTER JOIN deleted d ON i.asset_id = d.asset_id;
END
GO

IF OBJECT_ID('dbo.tr_audit_asset_details_crypto_crud', 'TR') IS NOT NULL
    DROP TRIGGER dbo.tr_audit_asset_details_crypto_crud;
GO
CREATE TRIGGER dbo.tr_audit_asset_details_crypto_crud
ON dbo.asset_details_crypto
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
    SELECT
        'asset_details_crypto',
        COALESCE(i.asset_id, d.asset_id),
        CASE
            WHEN i.asset_id IS NOT NULL AND d.asset_id IS NULL THEN 'CREATE'
            WHEN i.asset_id IS NOT NULL AND d.asset_id IS NOT NULL THEN 'UPDATE'
            WHEN i.asset_id IS NULL AND d.asset_id IS NOT NULL THEN 'DELETE'
        END,
        'CRUD operation captured by trigger',
        SYSUTCDATETIME()
    FROM inserted i
    FULL OUTER JOIN deleted d ON i.asset_id = d.asset_id;
END
GO

IF OBJECT_ID('dbo.tr_audit_transactions_crud', 'TR') IS NOT NULL
    DROP TRIGGER dbo.tr_audit_transactions_crud;
GO
CREATE TRIGGER dbo.tr_audit_transactions_crud
ON dbo.transactions
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
    SELECT
        'transactions',
        COALESCE(i.id, d.id),
        CASE
            WHEN i.id IS NOT NULL AND d.id IS NULL THEN 'CREATE'
            WHEN i.id IS NOT NULL AND d.id IS NOT NULL THEN 'UPDATE'
            WHEN i.id IS NULL AND d.id IS NOT NULL THEN 'DELETE'
        END,
        'CRUD operation captured by trigger',
        SYSUTCDATETIME()
    FROM inserted i
    FULL OUTER JOIN deleted d ON i.id = d.id;
END
GO

IF OBJECT_ID('dbo.tr_audit_asset_price_history_crud', 'TR') IS NOT NULL
    DROP TRIGGER dbo.tr_audit_asset_price_history_crud;
GO
CREATE TRIGGER dbo.tr_audit_asset_price_history_crud
ON dbo.asset_price_history
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
    SELECT
        'asset_price_history',
        COALESCE(i.id, d.id),
        CASE
            WHEN i.id IS NOT NULL AND d.id IS NULL THEN 'CREATE'
            WHEN i.id IS NOT NULL AND d.id IS NOT NULL THEN 'UPDATE'
            WHEN i.id IS NULL AND d.id IS NOT NULL THEN 'DELETE'
        END,
        'CRUD operation captured by trigger',
        SYSUTCDATETIME()
    FROM inserted i
    FULL OUTER JOIN deleted d ON i.id = d.id;
END
GO
