package com.finrisk.dao;

import com.finrisk.dto.response.Page;
import com.finrisk.model.Asset;
import com.finrisk.model.AssetType;

import java.util.List;

/** Catalog DAO covering polymorphic assets plus filtering helpers used by REST APIs. */
public interface AssetDao extends GenericDao<Asset, Long> {

    /** Retrieves an asset ignoring ticker casing differences. */
    Asset findBySymbolIgnoreCase(String symbol);

    /** Retrieves every asset tagged with a particular {@link AssetType}. */
    List<Asset> findByType(AssetType type);

    /** Applies combined filters (type, symbol equality, fuzzy name) with paging support. */
    Page<Asset> pageAssets(
            AssetType type,
            String symbolExact,
            String nameContains,
            int page,
            int size,
            List<String> sortSpecs);

    /** Updates the latest quoted price used by valuation services. */
    void updateCurrentPrice(long assetId, java.math.BigDecimal newPrice);
}
