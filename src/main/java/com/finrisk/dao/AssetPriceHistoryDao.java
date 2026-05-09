package com.finrisk.dao;

import com.finrisk.dto.response.AssetPricePoint;
import com.finrisk.dto.response.Page;

import java.math.BigDecimal;
import java.util.List;

public interface AssetPriceHistoryDao {

    void insert(long assetId, BigDecimal price);

    List<BigDecimal> latestPrices(long assetId, int limit);

    Page<AssetPricePoint> page(long assetId, int page, int size);
}
