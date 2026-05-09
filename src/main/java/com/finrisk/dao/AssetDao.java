package com.finrisk.dao;

import com.finrisk.dto.response.Page;
import com.finrisk.model.Asset;
import com.finrisk.model.AssetType;

import java.util.List;

public interface AssetDao extends GenericDao<Asset, Long> {

    Asset findBySymbolIgnoreCase(String symbol);

    List<Asset> findByType(AssetType type);

    Page<Asset> pageAssets(
            AssetType type,
            String symbolExact,
            String nameContains,
            int page,
            int size,
            List<String> sortSpecs);

    void updateCurrentPrice(long assetId, java.math.BigDecimal newPrice);
}
