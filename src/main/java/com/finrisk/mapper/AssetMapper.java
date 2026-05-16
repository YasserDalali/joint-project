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

/** Converts polymorphic {@link Asset} models into Jackson-friendly {@link AssetResponse} implementations. */
public final class AssetMapper {

    /** Prevents instantiation because mapping routines are entirely static. */
    private AssetMapper() {}

    /** Serializes any supported asset subtype into its matching response record with USD defaults. */
    public static AssetResponse toResponse(Asset a) {
        if (a instanceof Stock stock) {
            return new StockResponse(
                    stock.id(),
                    stock.symbol(),
                    stock.name(),
                    AssetType.STOCK,
                    stock.currentPrice(),
                    "USD",
                    stock.calculateRiskLevel(),
                    stock.createdAt(),
                    stock.sector(),
                    stock.exchange());
        }
        if (a instanceof ETF etf) {
            return new EtfResponse(
                    etf.id(),
                    etf.symbol(),
                    etf.name(),
                    AssetType.ETF,
                    etf.currentPrice(),
                    "USD",
                    etf.calculateRiskLevel(),
                    etf.createdAt(),
                    etf.issuer(),
                    etf.expenseRatio());
        }
        if (a instanceof Bond bond) {
            return new BondResponse(
                    bond.id(),
                    bond.symbol(),
                    bond.name(),
                    AssetType.BOND,
                    bond.currentPrice(),
                    "USD",
                    bond.calculateRiskLevel(),
                    bond.createdAt(),
                    bond.interestRate(),
                    bond.maturityDate(),
                    bond.issuer());
        }
        if (a instanceof CryptoAsset crypto) {
            return new CryptoResponse(
                    crypto.id(),
                    crypto.symbol(),
                    crypto.name(),
                    AssetType.CRYPTO,
                    crypto.currentPrice(),
                    "USD",
                    crypto.calculateRiskLevel(),
                    crypto.createdAt(),
                    crypto.blockchain());
        }
        throw new IllegalArgumentException("Unknown asset type: " + a.getClass().getName());
    }
}
