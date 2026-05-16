package com.finrisk.dao.impl;

import com.finrisk.dao.AssetDao;
import com.finrisk.dto.response.Page;
import com.finrisk.exception.DaoException;
import com.finrisk.exception.SymbolAlreadyExistsException;
import com.finrisk.factory.AssetFactory;
import com.finrisk.model.Asset;
import com.finrisk.model.AssetType;
import com.finrisk.model.Bond;
import com.finrisk.model.CryptoAsset;
import com.finrisk.model.ETF;
import com.finrisk.model.RiskLevel;
import com.finrisk.model.Stock;
import com.finrisk.util.Db;
import com.finrisk.util.SqlSort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** JDBC DAO hydrating polymorphic {@link Asset} records joined across subtype detail tables. */
@Repository
public class AssetDaoJdbc implements AssetDao {

    private static final String SELECT_BASE =
            """
            SELECT a.id, a.symbol, a.name, a.asset_type, a.current_price, a.risk_level, a.created_at,
                   s.sector, s.exchange_name,
                   e.issuer, e.expense_ratio,
                   b.interest_rate, b.maturity_date, b.issuer AS bond_issuer,
                   c.blockchain
            FROM dbo.assets a
            LEFT JOIN dbo.asset_details_stock s ON s.asset_id = a.id
            LEFT JOIN dbo.asset_details_etf e ON e.asset_id = a.id
            LEFT JOIN dbo.asset_details_bond b ON b.asset_id = a.id
            LEFT JOIN dbo.asset_details_crypto c ON c.asset_id = a.id
            """;

    private static final String INSERT_ASSET =
            """
            INSERT INTO dbo.assets (symbol, name, asset_type, current_price, risk_level, created_at)
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, ?, SYSUTCDATETIME())
            """;

    /** Recognizes duplicate symbol violations raised by SQL Server unique indexes. */
    private static boolean isUniqueViolation(SQLException e) {
        return "23000".equals(e.getSQLState()) || e.getErrorCode() == 2627 || e.getErrorCode() == 2601;
    }

    /** Converts joined catalog/detail columns into the proper sealed {@link Asset} implementation. */
    private static Asset mapAsset(ResultSet rs) throws SQLException {
        AssetType type = AssetType.valueOf(rs.getString("asset_type"));
        RiskLevel persisted = RiskLevel.valueOf(rs.getString("risk_level"));
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts == null ? null : ts.toLocalDateTime();
        long id = rs.getLong("id");
        String symbol = rs.getString("symbol");
        String name = rs.getString("name");
        BigDecimal price = rs.getBigDecimal("current_price");
        if (type == AssetType.STOCK) {
            return new Stock(
                    id,
                    symbol,
                    name,
                    price,
                    persisted,
                    createdAt,
                    rs.getString("sector"),
                    rs.getString("exchange_name"));
        }
        if (type == AssetType.ETF) {
            return new ETF(
                    id,
                    symbol,
                    name,
                    price,
                    persisted,
                    createdAt,
                    rs.getString("issuer"),
                    rs.getBigDecimal("expense_ratio"));
        }
        if (type == AssetType.BOND) {
            Date maturityDate = rs.getDate("maturity_date");
            LocalDate maturity =
                    maturityDate == null ? null : maturityDate.toLocalDate();
            return new Bond(
                    id,
                    symbol,
                    name,
                    price,
                    persisted,
                    createdAt,
                    rs.getBigDecimal("interest_rate"),
                    maturity,
                    rs.getString("bond_issuer"));
        }
        if (type == AssetType.CRYPTO) {
            return new CryptoAsset(
                    id, symbol, name, price, persisted, createdAt, rs.getString("blockchain"));
        }
        throw new IllegalStateException("Unknown asset type: " + type);
    }

    /** Loads one asset graph including subtype-specific columns by numeric id. */
    @Override
    public Asset findById(Long id) {
        Optional<Asset> found = Db.findOne(SELECT_BASE + " WHERE a.id = ?", rs -> mapAsset(rs), id);
        if (found.isPresent()) {
            return found.get();
        }
        return null;
    }

    /** Retrieves every instrument ordered by ascending asset id. */
    @Override
    public List<Asset> findAll() {
        return Db.findMany(SELECT_BASE + " ORDER BY a.id", rs -> mapAsset(rs));
    }

    /** Inserts master/detail asset rows transactionally using {@link Db#inTx}. */
    @Override
    public Asset save(Asset entity) {
        RiskLevel persisted = AssetFactory.persistedRisk(entity);
        try {
            return Db.inTx(
                    conn -> {
                        long assetId =
                                Db.insertReturning(
                                        conn,
                                        INSERT_ASSET,
                                        rs -> rs.getLong(1),
                                        entity.symbol(),
                                        entity.name(),
                                        entity.type().name(),
                                        entity.currentPrice(),
                                        persisted.name());
                        Asset staged = AssetFactory.withTimestamps(entity, assetId, null);
                        insertDetails(conn, staged);
                        Timestamp createdTs =
                                Db.findOne(
                                                conn,
                                                """
                                                SELECT created_at FROM dbo.assets WHERE id = ?
                                                """,
                                                rs -> rs.getTimestamp(1),
                                                assetId)
                                        .orElse(null);
                        LocalDateTime created =
                                createdTs == null ? null : createdTs.toLocalDateTime();
                        return AssetFactory.withTimestamps(entity, assetId, created);
                    });
        } catch (DaoException e) {
            if (e.getCause() instanceof SQLException sqlException && isUniqueViolation(sqlException)) {
                throw new SymbolAlreadyExistsException("Symbol already exists");
            }
            throw e;
        }
    }

    /** Persists subtype-specific columns immediately after the shared asset row exists. */
    private static void insertDetails(java.sql.Connection connection, Asset asset) {
        if (asset instanceof Stock stock) {
            Db.exec(
                    connection,
                    """
                    INSERT INTO dbo.asset_details_stock (asset_id, sector, exchange_name) VALUES (?,?,?)
                    """,
                    stock.id(),
                    stock.sector(),
                    stock.exchange());
            return;
        }
        if (asset instanceof ETF etf) {
            Object expenseRatio = etf.expenseRatio() == null ? Db.SqlNull.DECIMAL : etf.expenseRatio();
            Db.exec(
                    connection,
                    """
                    INSERT INTO dbo.asset_details_etf (asset_id, issuer, expense_ratio) VALUES (?,?,?)
                    """,
                    etf.id(),
                    etf.issuer(),
                    expenseRatio);
            return;
        }
        if (asset instanceof Bond bond) {
            Db.exec(
                    connection,
                    """
                    INSERT INTO dbo.asset_details_bond (asset_id, interest_rate, maturity_date, issuer) VALUES (?,?,?,?)
                    """,
                    bond.id(),
                    bond.interestRate(),
                    bond.maturityDate(),
                    bond.issuer());
            return;
        }
        if (asset instanceof CryptoAsset crypto) {
            Db.exec(
                    connection,
                    """
                    INSERT INTO dbo.asset_details_crypto (asset_id, blockchain) VALUES (?,?)
                    """,
                    crypto.id(),
                    crypto.blockchain());
            return;
        }
        throw new IllegalStateException("Unsupported asset type: " + asset.getClass().getName());
    }

    /** Explicitly unsupported because partial updates require dedicated APIs. */
    @Override
    public void update(Asset entity) {
        throw new UnsupportedOperationException("Use updateCurrentPrice or domain-specific updates");
    }

    /** Deletes an asset row cascading dependent rows per database rules. */
    @Override
    public void delete(Long id) {
        Db.exec(
                """
                DELETE FROM dbo.assets WHERE id = ?
                """,
                id);
    }

    /** Finds an asset using case-insensitive symbol equality across joined tables. */
    @Override
    public Asset findBySymbolIgnoreCase(String symbol) {
        Optional<Asset> found =
                Db.findOne(
                        SELECT_BASE + " WHERE LOWER(a.symbol) = LOWER(?)",
                        rs -> mapAsset(rs),
                        symbol.trim());
        if (found.isPresent()) {
            return found.get();
        }
        return null;
    }

    /** Lists assets filtered by coarse {@link AssetType}. */
    @Override
    public List<Asset> findByType(AssetType type) {
        return Db.findMany(
                SELECT_BASE + " WHERE a.asset_type = ? ORDER BY a.id", rs -> mapAsset(rs), type.name());
    }

    /** Applies stacked filters (type, symbol exact match, fuzzy name) with safe sorting + paging. */
    @Override
    public Page<Asset> pageAssets(
            AssetType type,
            String symbolExact,
            String nameContains,
            int page,
            int size,
            List<String> sortSpecs) {
        String order =
                SqlSort.orderByClause(sortSpecs, SqlSort.assetsWhitelist(), "a.created_at DESC, a.id DESC");
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (type != null) {
            where.append(" AND a.asset_type = ? ");
            params.put("t", type.name());
        }
        if (symbolExact != null && !symbolExact.isBlank()) {
            where.append(" AND LOWER(a.symbol) = LOWER(?) ");
            params.put("s", symbolExact.trim());
        }
        if (nameContains != null && !nameContains.isBlank()) {
            where.append(" AND LOWER(a.name) LIKE '%' + LOWER(?) + '%' ESCAPE '\\' ");
            params.put("n", escapeLike(nameContains.trim()));
        }
        String countSql = "SELECT COUNT(1) FROM dbo.assets a " + where;
        String dataSql =
                SELECT_BASE + where + " ORDER BY " + order + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return Db.findPage(
                countSql,
                dataSql,
                rs -> mapAsset(rs),
                page,
                size,
                ps -> bindAssetFilters(ps, params),
                ps -> {
                    int idx = bindAssetFilters(ps, params);
                    ps.setInt(idx++, page * size);
                    ps.setInt(idx, size);
                });
    }

    /** Binds dynamic WHERE parameters built during {@link #pageAssets} assembly. */
    private static int bindAssetFilters(java.sql.PreparedStatement ps, Map<String, Object> params)
            throws SQLException {
        int idx = 1;
        if (params.containsKey("t")) {
            ps.setString(idx++, (String) params.get("t"));
        }
        if (params.containsKey("s")) {
            ps.setString(idx++, (String) params.get("s"));
        }
        if (params.containsKey("n")) {
            ps.setString(idx++, (String) params.get("n"));
        }
        return idx;
    }

    /** Escapes substring search inputs destined for T-SQL LIKE expressions with ESCAPE {@code \\}. */
    private static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Updates only {@code current_price} on the master assets row. */
    @Override
    public void updateCurrentPrice(long assetId, BigDecimal newPrice) {
        Db.update(
                """
                UPDATE dbo.assets SET current_price = ? WHERE id = ?
                """,
                newPrice,
                assetId);
    }
}
