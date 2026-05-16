package com.finrisk.factory;

import com.finrisk.dto.request.AssetCreateRequest;
import com.finrisk.dto.request.BondCreateRequest;
import com.finrisk.dto.request.CryptoCreateRequest;
import com.finrisk.dto.request.EtfCreateRequest;
import com.finrisk.dto.request.StockCreateRequest;
import com.finrisk.model.Asset;
import com.finrisk.model.Bond;
import com.finrisk.model.CryptoAsset;
import com.finrisk.model.ETF;
import com.finrisk.model.RiskLevel;
import com.finrisk.model.Stock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Factory Method helper converting polymorphic {@link com.finrisk.dto.request.AssetCreateRequest} payloads into domain assets. */
public final class AssetFactory {

    /** Blocks instantiation because creation flows use static factory methods exclusively. */
    private AssetFactory() {}

    /** Materializes the correct {@link Asset} subtype prior to JDBC persistence. */
    public static Asset create(AssetCreateRequest req) {
        if (req instanceof StockCreateRequest stockReq) {
            return new Stock(
                    null,
                    stockReq.symbol().trim(),
                    stockReq.name().trim(),
                    stockReq.currentPrice(),
                    RiskLevel.HIGH,
                    null,
                    stockReq.sector(),
                    stockReq.exchange());
        }
        if (req instanceof EtfCreateRequest etfReq) {
            return new ETF(
                    null,
                    etfReq.symbol().trim(),
                    etfReq.name().trim(),
                    etfReq.currentPrice(),
                    RiskLevel.MEDIUM,
                    null,
                    etfReq.issuer(),
                    etfReq.expenseRatio());
        }
        if (req instanceof BondCreateRequest bondReq) {
            return new Bond(
                    null,
                    bondReq.symbol().trim(),
                    bondReq.name().trim(),
                    bondReq.currentPrice(),
                    RiskLevel.LOW,
                    null,
                    bondReq.interestRate(),
                    bondReq.maturityDate(),
                    bondReq.issuer());
        }
        if (req instanceof CryptoCreateRequest cryptoReq) {
            return new CryptoAsset(
                    null,
                    cryptoReq.symbol().trim(),
                    cryptoReq.name().trim(),
                    cryptoReq.currentPrice(),
                    RiskLevel.VERY_HIGH,
                    null,
                    cryptoReq.blockchain());
        }
        throw new IllegalArgumentException("Unknown asset request type: " + req.getClass().getName());
    }

    /** Rebuilds an asset value object after the database assigns id/timestamps. */
    public static Asset withTimestamps(Asset a, long id, LocalDateTime createdAt) {
        if (a instanceof Stock stock) {
            return new Stock(
                    id,
                    stock.symbol(),
                    stock.name(),
                    stock.currentPrice(),
                    stock.riskLevel(),
                    createdAt,
                    stock.sector(),
                    stock.exchange());
        }
        if (a instanceof ETF etf) {
            return new ETF(
                    id,
                    etf.symbol(),
                    etf.name(),
                    etf.currentPrice(),
                    etf.riskLevel(),
                    createdAt,
                    etf.issuer(),
                    etf.expenseRatio());
        }
        if (a instanceof Bond bond) {
            return new Bond(
                    id,
                    bond.symbol(),
                    bond.name(),
                    bond.currentPrice(),
                    bond.riskLevel(),
                    createdAt,
                    bond.interestRate(),
                    bond.maturityDate(),
                    bond.issuer());
        }
        if (a instanceof CryptoAsset crypto) {
            return new CryptoAsset(
                    id,
                    crypto.symbol(),
                    crypto.name(),
                    crypto.currentPrice(),
                    crypto.riskLevel(),
                    createdAt,
                    crypto.blockchain());
        }
        throw new IllegalArgumentException("Unknown asset type: " + a.getClass().getName());
    }

    /** Reads the persisted coarse risk tier derived from domain rules. */
    public static RiskLevel persistedRisk(Asset a) {
        return a.calculateRiskLevel();
    }

    /** Normalizes nullable ETF expense ratios before JDBC binding. */
    public static BigDecimal safeExpenseRatio(ETF etf) {
        if (etf.expenseRatio() == null) {
            return BigDecimal.ZERO;
        }
        return etf.expenseRatio();
    }
}
