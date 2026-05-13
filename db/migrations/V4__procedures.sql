/*
  Stored procedures: buy/sell - balance, trade row, audit log.

  SET NOCOUNT ON : kat7yed messages "(x rows affected)" 

  SET XACT_ABORT ON : ila error f transaction, rollback behavior wadh7 m3a TRY/CATCH.

  UPDLOCK + HOLDLOCK : locks 3la rows - katmna3 jouj transactions yqraw nefs balance yktbou ghalt (race)

  CAST(quantity AS DECIMAL) : nefs 3illa dyal schema - flous w quantities f calculations khasso DECIMAL.

  BEGIN TRAN / TRY / CATCH / THROW : all-or-nothing writes; THROW katrj3 nefs error l asliya.

  SCOPE_IDENTITY() : ID dyal akher INSERT f nafs scope - audit yrbt b transaction s7i7a.

  RAISERROR('CODE', ...) : string messages for errors.

  OBJECT_ID(..., 'P') : P = procedure; drop/create idempotent.

  Ma n3mloch update/insert "simple" bla locks f financial flows - consistency aham men brevity.
*/

IF OBJECT_ID('dbo.sp_buy_asset', 'P') IS NOT NULL
    DROP PROCEDURE dbo.sp_buy_asset;
GO

CREATE PROCEDURE dbo.sp_buy_asset -- ATOMIC all or nothing
    @account_id BIGINT,
    @asset_id BIGINT,
    @quantity INT,
    @unit_price DECIMAL(19,4)
AS
BEGIN
    -- Kan7ydo messages b7al "(x rows affected)".
    SET NOCOUNT ON;
    -- Ila tra error f transaction, SQL Server kaydir rollback auto.
    SET XACT_ABORT ON;

    DECLARE @total DECIMAL(19,4) = CAST(@quantity AS DECIMAL(19,4)) * @unit_price;
    DECLARE @cash DECIMAL(19,4);
    DECLARE @new_tx_id BIGINT;

    -- UPDLOCK + HOLDLOCK: lock lrow dyal account bach ntfado race conditions.
    SELECT @cash = cash_balance FROM dbo.accounts WITH (UPDLOCK, HOLDLOCK) WHERE id = @account_id;
    IF @cash IS NULL
    BEGIN
        RAISERROR('ACCOUNT_NOT_FOUND', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (SELECT 1 FROM dbo.assets WHERE id = @asset_id)
    BEGIN
        RAISERROR('ASSET_NOT_FOUND', 16, 1);
        RETURN;
    END

    IF @cash < @total
    BEGIN
        RAISERROR('INSUFFICIENT_BALANCE', 16, 1);
        RETURN;
    END

    BEGIN TRANSACTION;

    BEGIN TRY
        UPDATE dbo.accounts
        SET cash_balance = cash_balance - @total
        WHERE id = @account_id;

        INSERT INTO dbo.transactions (account_id, asset_id, transaction_type, quantity, unit_price, transaction_date)
        VALUES (@account_id, @asset_id, 'BUY', @quantity, @unit_price, SYSUTCDATETIME());

        -- SCOPE_IDENTITY() katrj3 ID li tzad b akher INSERT f nafs scope.
        SET @new_tx_id = SCOPE_IDENTITY();

        INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
        VALUES ('TRANSACT' + 'ION', @new_tx_id, 'BUY_TRANSACTION_CREATED', 'Buy trade persisted', SYSUTCDATETIME());

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        -- THROW katrmi nafs l error l asliy (number/state/message).
        THROW;
    END CATCH
END
GO

IF OBJECT_ID('dbo.sp_sell_asset', 'P') IS NOT NULL
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

    -- Kan-lockiw check dyal account bach ykoun mtnase9 m3a concurrent updates.
    IF NOT EXISTS (SELECT 1 FROM dbo.accounts WITH (UPDLOCK, HOLDLOCK) WHERE id = @account_id)
    BEGIN
        RAISERROR('ACCOUNT_NOT_FOUND', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (SELECT 1 FROM dbo.assets WHERE id = @asset_id)
    BEGIN
        RAISERROR('ASSET_NOT_FOUND', 16, 1);
        RETURN;
    END

    SELECT @owned = SUM(CASE WHEN t.transaction_type = 'BUY' THEN t.quantity ELSE -t.quantity END)
    FROM dbo.transactions t WITH (UPDLOCK, HOLDLOCK)
    WHERE t.account_id = @account_id AND t.asset_id = @asset_id;

    IF @owned IS NULL OR @owned < @quantity
    BEGIN
        RAISERROR('INSUFFICIENT_QUANTITY', 16, 1);
        RETURN;
    END

    BEGIN TRANSACTION;

    BEGIN TRY
        UPDATE dbo.accounts
        SET cash_balance = cash_balance + @total
        WHERE id = @account_id;

        INSERT INTO dbo.transactions (account_id, asset_id, transaction_type, quantity, unit_price, transaction_date)
        VALUES (@account_id, @asset_id, 'SELL', @quantity, @unit_price, SYSUTCDATETIME());

        SET @new_tx_id = SCOPE_IDENTITY();

        INSERT INTO dbo.audit_logs (entity_name, entity_id, action_type, description, created_at)
        VALUES ('TRANSACT' + 'ION', @new_tx_id, 'SELL_TRANSACTION_CREATED', 'Sell trade persisted', SYSUTCDATETIME());

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END
GO
