package com.finrisk.dao;

import com.finrisk.dto.response.AssetPricePoint;
import com.finrisk.dto.response.Page;

import java.math.BigDecimal;
import java.util.List;

/** Append-only style history table access supporting charts and volatility Strategy inputs. */
public interface AssetPriceHistoryDao {

    /** Records a new observation into the historical price series for an asset. */
    void insert(long assetId, BigDecimal price);

    /** Fetches the newest {@code limit} prices ordered from fresh to stale. */
    List<BigDecimal> latestPrices(long assetId, int limit);

    /** Delivers paginated {@link AssetPricePoint} rows suitable for REST consumers. */
    Page<AssetPricePoint> page(long assetId, int page, int size);
}
