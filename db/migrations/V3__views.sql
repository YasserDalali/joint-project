/*
  Portfolio views (holdings, account summary, P&L per open position).
  Uses subqueries instead of WITH ... so it reads top-to-bottom like junior SQL.
*/

IF OBJECT_ID('dbo.vw_portfolio_holdings', 'V') IS NOT NULL
    DROP VIEW dbo.vw_portfolio_holdings;
GO

CREATE VIEW dbo.vw_portfolio_holdings
AS
SELECT
    q.account_id,
    q.asset_id,
    a.symbol,
    a.name,
    a.asset_type,
    q.quantity,
    a.current_price,
    CAST(q.quantity AS DECIMAL(19, 4)) * a.current_price AS current_value
FROM (
    SELECT
        account_id,
        asset_id,
        SUM(CASE WHEN transaction_type = 'BUY' THEN quantity ELSE -quantity END) AS quantity
    FROM dbo.transactions
    GROUP BY account_id, asset_id
) AS q
INNER JOIN dbo.assets AS a ON a.id = q.asset_id
WHERE q.quantity > 0;
GO

IF OBJECT_ID('dbo.vw_portfolio_summary', 'V') IS NOT NULL
    DROP VIEW dbo.vw_portfolio_summary;
GO

CREATE VIEW dbo.vw_portfolio_summary
AS
SELECT
    ac.id AS account_id,
    ac.cash_balance,
    COALESCE(SUM(ph.current_value), CAST(0 AS DECIMAL(19, 4))) AS total_holdings_value,
    ac.cash_balance + COALESCE(SUM(ph.current_value), CAST(0 AS DECIMAL(19, 4))) AS total_account_value
FROM dbo.accounts AS ac
LEFT JOIN dbo.vw_portfolio_holdings AS ph ON ph.account_id = ac.id
GROUP BY ac.id, ac.cash_balance;
GO

IF OBJECT_ID('dbo.vw_portfolio_profit_loss', 'V') IS NOT NULL
    DROP VIEW dbo.vw_portfolio_profit_loss;
GO

CREATE VIEW dbo.vw_portfolio_profit_loss
AS
SELECT
    h.account_id,
    h.asset_id,
    ast.symbol,
    h.quantity,
    tt.buy_cost - tt.sell_proceeds AS net_invested,
    CAST(h.quantity AS DECIMAL(19, 4)) * ast.current_price AS current_value,
    (CAST(h.quantity AS DECIMAL(19, 4)) * ast.current_price) - (tt.buy_cost - tt.sell_proceeds) AS profit_loss,
    CASE
        WHEN (tt.buy_cost - tt.sell_proceeds) = 0 THEN NULL
        ELSE (
            (CAST(h.quantity AS DECIMAL(19, 4)) * ast.current_price) - (tt.buy_cost - tt.sell_proceeds)
        ) / NULLIF(tt.buy_cost - tt.sell_proceeds, 0) * 100.0
    END AS profit_loss_percent
FROM dbo.vw_portfolio_holdings AS h
INNER JOIN dbo.assets AS ast ON ast.id = h.asset_id
INNER JOIN (
    SELECT
        account_id,
        asset_id,
        SUM(CASE WHEN transaction_type = 'BUY' THEN quantity * unit_price ELSE 0 END) AS buy_cost,
        SUM(CASE WHEN transaction_type = 'SELL' THEN quantity * unit_price ELSE 0 END) AS sell_proceeds
    FROM dbo.transactions
    GROUP BY account_id, asset_id
) AS tt ON tt.account_id = h.account_id AND tt.asset_id = h.asset_id;
GO
