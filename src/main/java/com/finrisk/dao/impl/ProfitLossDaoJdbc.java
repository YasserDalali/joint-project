package com.finrisk.dao.impl;

import com.finrisk.dao.ProfitLossDao;
import com.finrisk.dto.response.HoldingProfitLoss;
import com.finrisk.util.Db;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ProfitLossDaoJdbc implements ProfitLossDao {

    @Override
    public List<HoldingProfitLoss> holdings(long accountId) {
        return Db.findMany(
                """
                SELECT asset_id, symbol, quantity, net_invested, current_value, profit_loss, profit_loss_percent
                FROM dbo.vw_portfolio_profit_loss
                WHERE account_id = ?
                ORDER BY symbol
                """,
                ProfitLossDaoJdbc::mapRow,
                accountId);
    }

    private static HoldingProfitLoss mapRow(ResultSet rs) throws SQLException {
        BigDecimal pct = rs.getBigDecimal("profit_loss_percent");
        return new HoldingProfitLoss(
                rs.getLong("asset_id"),
                rs.getString("symbol"),
                rs.getBigDecimal("quantity").intValue(),
                rs.getBigDecimal("net_invested"),
                rs.getBigDecimal("current_value"),
                rs.getBigDecimal("profit_loss"),
                pct);
    }
}
