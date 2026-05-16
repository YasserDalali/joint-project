package com.finrisk.dao.impl;

import com.finrisk.dao.AssetPriceHistoryDao;
import com.finrisk.dto.response.AssetPricePoint;
import com.finrisk.dto.response.Page;
import com.finrisk.util.Db;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/** Persists and reads rows from {@code dbo.asset_price_history} backing volatility analytics. */
@Repository
public class AssetPriceHistoryDaoJdbc implements AssetPriceHistoryDao {

    /** Appends a timestamped price observation linked to an asset id. */
    @Override
    public void insert(long assetId, BigDecimal price) {
        Db.exec(
                """
                INSERT INTO dbo.asset_price_history (asset_id, price, recorded_at) VALUES (?, ?, SYSUTCDATETIME())
                """,
                assetId,
                price);
    }

    /** Retrieves the freshest {@code limit} prices ordered newest-first as bare decimals. */
    @Override
    public List<BigDecimal> latestPrices(long assetId, int limit) {
        return Db.findMany(
                """
                SELECT TOP (?) price FROM dbo.asset_price_history WHERE asset_id = ? ORDER BY recorded_at DESC
                """,
                rs -> rs.getBigDecimal(1),
                Math.max(limit, 1),
                assetId);
    }

    /** Pages detailed {@link AssetPricePoint} rows including timestamps for REST consumers. */
    @Override
    public Page<AssetPricePoint> page(long assetId, int page, int size) {
        String countSql =
                """
                SELECT COUNT(1) FROM dbo.asset_price_history WHERE asset_id = ?
                """;
        String dataSql =
                """
                SELECT price, recorded_at FROM dbo.asset_price_history WHERE asset_id = ? ORDER BY recorded_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        return Db.findPage(
                countSql,
                dataSql,
                rs -> {
                    Timestamp ts = rs.getTimestamp("recorded_at");
                    return new AssetPricePoint(
                            rs.getBigDecimal("price"),
                            ts == null ? null : ts.toLocalDateTime());
                },
                page,
                size,
                ps -> ps.setLong(1, assetId),
                ps -> {
                    ps.setLong(1, assetId);
                    ps.setInt(2, page * size);
                    ps.setInt(3, size);
                });
    }
}
