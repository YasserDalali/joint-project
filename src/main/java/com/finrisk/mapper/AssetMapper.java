package com.finrisk.mapper;

import com.finrisk.dto.response.AssetResponse;
import com.finrisk.dto.response.BondResponse;
import com.finrisk.dto.response.CryptoResponse;
import com.finrisk.dto.response.EtfResponse;
import com.finrisk.dto.response.StockResponse;
import com.finrisk.model.Asset;
import com.finrisk.model.AssetType;
import com.finrisk.model.Bond;
import com.finrisk.model.CryptoAsset;
import com.finrisk.model.ETF;
import com.finrisk.model.Stock;

public final class AssetMapper {

    private AssetMapper() {}

    public static AssetResponse toResponse(Asset a) {
        return switch (a) {
            case Stock s -> new StockResponse(
                    s.id(),
                    s.symbol(),
                    s.name(),
                    AssetType.STOCK,
                    s.currentPrice(),
                    "USD",
                    s.calculateRiskLevel(),
                    s.createdAt(),
                    s.sector(),
                    s.exchange());
            case ETF e -> new EtfResponse(
                    e.id(),
                    e.symbol(),
                    e.name(),
                    AssetType.ETF,
                    e.currentPrice(),
                    "USD",
                    e.calculateRiskLevel(),
                    e.createdAt(),
                    e.issuer(),
                    e.expenseRatio());
            case Bond b -> new BondResponse(
                    b.id(),
                    b.symbol(),
                    b.name(),
                    AssetType.BOND,
                    b.currentPrice(),
                    "USD",
                    b.calculateRiskLevel(),
                    b.createdAt(),
                    b.interestRate(),
                    b.maturityDate(),
                    b.issuer());
            case CryptoAsset c -> new CryptoResponse(
                    c.id(),
                    c.symbol(),
                    c.name(),
                    AssetType.CRYPTO,
                    c.currentPrice(),
                    "USD",
                    c.calculateRiskLevel(),
                    c.createdAt(),
                    c.blockchain());
        };
    }
}
