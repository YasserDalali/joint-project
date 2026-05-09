/* Portfolio and P&L reporting views */

IF OBJECT_ID(N'dbo.vw_portfolio_holdings', N'V') IS NOT NULL
    DROP VIEW dbo.vw_portfolio_holdings;
GO

CREATE VIEW dbo.vw_portfolio_holdings
AS
WITH net_qty AS (
    SELECT
        t.account_id,
        t.asset_id,
        SUM(CASE WHEN t.transaction_type = N'BUY' THEN t.quantity ELSE -t.quantity END) AS quantity
    FROM dbo.transactions t
    GROUP BY t.account_id, t.asset_id
)
SELECT
    n.account_id,
    n.asset_id,
    a.symbol,
    a.name,
    a.asset_type,
    n.quantity,
    a.current_price,
    CAST(n.quantity AS DECIMAL(19,4)) * a.current_price AS current_value
FROM net_qty n
INNER JOIN dbo.assets a ON a.id = n.asset_id
WHERE n.quantity > 0;
GO

IF OBJECT_ID(N'dbo.vw_portfolio_summary', N'V') IS NOT NULL
    DROP VIEW dbo.vw_portfolio_summary;
GO

CREATE VIEW dbo.vw_portfolio_summary
AS
SELECT
    ac.id AS account_id,
    ac.cash_balance,
    COALESCE(SUM(ph.current_value), CAST(0 AS DECIMAL(19,4))) AS total_holdings_value,
    ac.cash_balance + COALESCE(SUM(ph.current_value), CAST(0 AS DECIMAL(19,4))) AS total_account_value
FROM dbo.accounts ac
LEFT JOIN dbo.vw_portfolio_holdings ph ON ph.account_id = ac.id
GROUP BY ac.id, ac.cash_balance;
GO

IF OBJECT_ID(N'dbo.vw_portfolio_profit_loss', N'V') IS NOT NULL
    DROP VIEW dbo.vw_portfolio_profit_loss;
GO

CREATE VIEW dbo.vw_portfolio_profit_loss
AS
WITH trade_totals AS (
    SELECT
        t.account_id,
        t.asset_id,
        SUM(CASE WHEN t.transaction_type = N'BUY' THEN t.quantity * t.unit_price ELSE 0 END) AS buy_cost,
        SUM(CASE WHEN t.transaction_type = N'SELL' THEN t.quantity * t.unit_price ELSE 0 END) AS sell_proceeds
    FROM dbo.transactions t
    GROUP BY t.account_id, t.asset_id
),
holdings AS (
    SELECT
        t.account_id,
        t.asset_id,
        SUM(CASE WHEN t.transaction_type = N'BUY' THEN t.quantity ELSE -t.quantity END) AS quantity
    FROM dbo.transactions t
    GROUP BY t.account_id, t.asset_id
    HAVING SUM(CASE WHEN t.transaction_type = N'BUY' THEN t.quantity ELSE -t.quantity END) > 0
)
SELECT
    h.account_id,
    h.asset_id,
    ast.symbol,
    h.quantity,
    tt.buy_cost - tt.sell_proceeds AS net_invested,
    CAST(h.quantity AS DECIMAL(19,4)) * ast.current_price AS current_value,
    (CAST(h.quantity AS DECIMAL(19,4)) * ast.current_price) - (tt.buy_cost - tt.sell_proceeds) AS profit_loss,
    CASE
        WHEN (tt.buy_cost - tt.sell_proceeds) = 0 THEN NULL
        ELSE (((CAST(h.quantity AS DECIMAL(19,4)) * ast.current_price) - (tt.buy_cost - tt.sell_proceeds))
              / NULLIF(tt.buy_cost - tt.sell_proceeds, 0)) * 100.0
    END AS profit_loss_percent
FROM holdings h
INNER JOIN dbo.assets ast ON ast.id = h.asset_id
INNER JOIN trade_totals tt
    ON tt.account_id = h.account_id AND tt.asset_id = h.asset_id;
GO
