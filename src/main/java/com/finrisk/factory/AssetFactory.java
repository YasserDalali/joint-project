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

public final class AssetFactory {

    private AssetFactory() {}

    public static Asset create(AssetCreateRequest req) {
        return switch (req) {
            case StockCreateRequest s -> new Stock(
                    null,
                    s.symbol().trim(),
                    s.name().trim(),
                    s.currentPrice(),
                    RiskLevel.HIGH,
                    null,
                    s.sector(),
                    s.exchange());
            case EtfCreateRequest e -> new ETF(
                    null,
                    e.symbol().trim(),
                    e.name().trim(),
                    e.currentPrice(),
                    RiskLevel.MEDIUM,
                    null,
                    e.issuer(),
                    e.expenseRatio());
            case BondCreateRequest b -> new Bond(
                    null,
                    b.symbol().trim(),
                    b.name().trim(),
                    b.currentPrice(),
                    RiskLevel.LOW,
                    null,
                    b.interestRate(),
                    b.maturityDate(),
                    b.issuer());
            case CryptoCreateRequest c -> new CryptoAsset(
                    null,
                    c.symbol().trim(),
                    c.name().trim(),
                    c.currentPrice(),
                    RiskLevel.VERY_HIGH,
                    null,
                    c.blockchain());
        };
    }

    public static Asset withTimestamps(Asset a, long id, LocalDateTime createdAt) {
        return switch (a) {
            case Stock s -> new Stock(
                    id,
                    s.symbol(),
                    s.name(),
                    s.currentPrice(),
                    s.riskLevel(),
                    createdAt,
                    s.sector(),
                    s.exchange());
            case ETF e -> new ETF(
                    id,
                    e.symbol(),
                    e.name(),
                    e.currentPrice(),
                    e.riskLevel(),
                    createdAt,
                    e.issuer(),
                    e.expenseRatio());
            case Bond b -> new Bond(
                    id,
                    b.symbol(),
                    b.name(),
                    b.currentPrice(),
                    b.riskLevel(),
                    createdAt,
                    b.interestRate(),
                    b.maturityDate(),
                    b.issuer());
            case CryptoAsset c -> new CryptoAsset(
                    id,
                    c.symbol(),
                    c.name(),
                    c.currentPrice(),
                    c.riskLevel(),
                    createdAt,
                    c.blockchain());
        };
    }

    public static RiskLevel persistedRisk(Asset a) {
        return a.calculateRiskLevel();
    }

    public static BigDecimal safeExpenseRatio(ETF etf) {
        return etf.expenseRatio() == null ? BigDecimal.ZERO : etf.expenseRatio();
    }
}
