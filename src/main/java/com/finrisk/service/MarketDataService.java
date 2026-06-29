package com.finrisk.service;

import com.finrisk.dao.AssetDao;
import com.finrisk.dao.AssetPriceHistoryDao;
import com.finrisk.exception.AssetNotFoundException;
import com.finrisk.marketdata.MarketDataProvider;
import com.finrisk.marketdata.PricePoint;
import com.finrisk.model.Asset;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarketDataService {

    private final MarketDataProvider provider;
    private final AssetDao assetDao;
    private final AssetPriceHistoryDao priceHistoryDao;

    public MarketDataService(
            MarketDataProvider provider,
            AssetDao assetDao,
            AssetPriceHistoryDao priceHistoryDao) {
        this.provider = provider;
        this.assetDao = assetDao;
        this.priceHistoryDao = priceHistoryDao;
    }

    public int syncPrices(long assetId, int days) {
        Asset asset = assetDao.findById(assetId);
        if (asset == null) {
            throw new AssetNotFoundException("Asset not found: " + assetId);
        }
        List<PricePoint> points = provider.fetchDailyPrices(asset.symbol(), days);
        for (PricePoint p : points) {
            priceHistoryDao.upsertForDate(assetId, p.date(), p.close());
        }
        return points.size();
    }
}
