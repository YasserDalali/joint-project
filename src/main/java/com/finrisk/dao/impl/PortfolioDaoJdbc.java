package com.finrisk.dao.impl;

import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.Holding;
import com.finrisk.model.AssetType;
import com.finrisk.util.Db;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class PortfolioDaoJdbc implements PortfolioDao {

    @Override
    public Optional<BigDecimal> cashBalance(long accountId) {
        return Db.findOne(
                """
                SELECT cash_balance FROM dbo.accounts WHERE id = ?
                """,
                rs -> rs.getBigDecimal(1),
                accountId);
    }

    @Override
    public List<Holding> holdings(long accountId) {
        return Db.findMany(
                """
                SELECT asset_id, symbol, name, asset_type, quantity, current_price, current_value
                FROM dbo.vw_portfolio_holdings
                WHERE account_id = ?
                ORDER BY symbol
                """,
                PortfolioDaoJdbc::mapHolding,
                accountId);
    }

    private static Holding mapHolding(ResultSet rs) throws SQLException {
        return new Holding(
                rs.getLong("asset_id"),
                rs.getString("symbol"),
                rs.getString("name"),
                AssetType.valueOf(rs.getString("asset_type")),
                rs.getBigDecimal("quantity").intValue(),
                rs.getBigDecimal("current_price"),
                rs.getBigDecimal("current_value"));
    }
}
