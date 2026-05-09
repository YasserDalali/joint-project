/* Buy / sell stored procedures — orchestrate balance, trade row, audit log */

IF OBJECT_ID(N'dbo.sp_buy_asset', N'P') IS NOT NULL
    DROP PROCEDURE dbo.sp_buy_asset;
GO

CREATE PROCEDURE dbo.sp_buy_asset
    @account_id BIGINT,
    @asset_id BIGINT,
    @quantity INT,
    @unit_price DECIMAL(19,4)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @total DECIMAL(19,4) = CAST(@quantity AS DECIMAL(19,4)) * @unit_price;
    DECLARE @cash DECIMAL(19,4);
    DECLARE @new_tx_id BIGINT;

    SELECT @cash = cash_balance FROM dbo.accounts WITH (UPDLOCK, HOLDLOCK) WHERE id = @account_id;
    IF @cash IS NULL
    BEGIN
        RAISERROR(N'ACCOUNT_NOT_FOUND', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (SELECT 1 FROM dbo.assets WHERE id = @asset_id)
    BEGIN
        RAISERROR(N'ASSET_NOT_FOUND', 16, 1);
        RETURN;
    END

    IF @cash < @total
    BEGIN
        RAISERROR(N'INSUFFICIENT_BALANCE', 16, 1);
        RETURN;
    END

    BEGIN TRANSACTION;

    BEGIN TRY
        UPDATE dbo.accounts
        SET cash_balance = cash_balance - @total
        WHERE id = @account_id;

        INSERT INTO dbo.transactions (account_id, asset_id, transaction_type, quantity, unit_price, transaction_date)
        VALUES (@account_id, @asset_id, N'BUY', @quantity, @unit_price, SYSUTCDATETIME());

        SET @new_tx_id = SCOPE_IDENTITY();

        INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
        VALUES (N'TRANSACTION', @new_tx_id, N'BUY_TRANSACTION_CREATED', N'Buy trade persisted', SYSUTCDATETIME());

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END
GO

IF OBJECT_ID(N'dbo.sp_sell_asset', N'P') IS NOT NULL
    DROP PROCEDURE dbo.sp_sell_asset;
GO

CREATE PROCEDURE dbo.sp_sell_asset
    @account_id BIGINT,
    @asset_id BIGINT,
    @quantity INT,
    @unit_price DECIMAL(19,4)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @total DECIMAL(19,4) = CAST(@quantity AS DECIMAL(19,4)) * @unit_price;
    DECLARE @owned INT;
    DECLARE @new_tx_id BIGINT;

    IF NOT EXISTS (SELECT 1 FROM dbo.accounts WITH (UPDLOCK, HOLDLOCK) WHERE id = @account_id)
    BEGIN
        RAISERROR(N'ACCOUNT_NOT_FOUND', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (SELECT 1 FROM dbo.assets WHERE id = @asset_id)
    BEGIN
        RAISERROR(N'ASSET_NOT_FOUND', 16, 1);
        RETURN;
    END

    SELECT @owned = SUM(CASE WHEN t.transaction_type = N'BUY' THEN t.quantity ELSE -t.quantity END)
    FROM dbo.transactions t WITH (UPDLOCK, HOLDLOCK)
    WHERE t.account_id = @account_id AND t.asset_id = @asset_id;

    IF @owned IS NULL OR @owned < @quantity
    BEGIN
        RAISERROR(N'INSUFFICIENT_QUANTITY', 16, 1);
        RETURN;
    END

    BEGIN TRANSACTION;

    BEGIN TRY
        UPDATE dbo.accounts
        SET cash_balance = cash_balance + @total
        WHERE id = @account_id;

        INSERT INTO dbo.transactions (account_id, asset_id, transaction_type, quantity, unit_price, transaction_date)
        VALUES (@account_id, @asset_id, N'SELL', @quantity, @unit_price, SYSUTCDATETIME());

        SET @new_tx_id = SCOPE_IDENTITY();

        INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
        VALUES (N'TRANSACTION', @new_tx_id, N'SELL_TRANSACTION_CREATED', N'Sell trade persisted', SYSUTCDATETIME());

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END
GO
