package com.finrisk.service;

import com.finrisk.dao.AssetDao;
import com.finrisk.dao.AssetPriceHistoryDao;
import com.finrisk.dto.request.AssetCreateRequest;
import com.finrisk.dto.request.AssetPriceUpdateRequest;
import com.finrisk.dto.response.AssetPricePoint;
import com.finrisk.dto.response.AssetResponse;
import com.finrisk.dto.response.Page;
import com.finrisk.exception.AssetNotFoundException;
import com.finrisk.factory.AssetFactory;
import com.finrisk.mapper.AssetMapper;
import com.finrisk.model.Asset;
import com.finrisk.model.AssetType;
import com.finrisk.util.SqlSort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Handles catalog CRUD, price refreshes, and historical queries layered over asset DAOs. */
@Service
public class AssetService {

    private static final String ASSET_NOT_FOUND = "Asset not found";

    private final AssetDao assetDao;
    private final AssetPriceHistoryDao assetPriceHistoryDao;

    /** Supplies repositories for assets and their optional price history timeline. */
    public AssetService(AssetDao assetDao, AssetPriceHistoryDao assetPriceHistoryDao) {
        this.assetDao = assetDao;
        this.assetPriceHistoryDao = assetPriceHistoryDao;
    }

    /** Persists a polymorphic asset definition plus its initial history sample. */
    public AssetResponse createAsset(AssetCreateRequest req) {
        Asset asset = AssetFactory.create(req);
        Asset saved = assetDao.save(asset);
        assetPriceHistoryDao.insert(saved.id(), saved.currentPrice());
        return AssetMapper.toResponse(assetDao.findById(saved.id()));
    }

    /** Retrieves one catalog asset by id mapped to {@link AssetResponse}. */
    public AssetResponse getAsset(long id) {
        Asset asset = assetDao.findById(id);
        if (asset == null) {
            throw new AssetNotFoundException(ASSET_NOT_FOUND);
        }
        return AssetMapper.toResponse(asset);
    }

    /** Applies combined filters to asset search results with pagination/sorting. */
    public Page<AssetResponse> listAssets(
            AssetType type, String symbol, String search, int page, int size, List<String> sort) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<Asset> assetPage =
                assetDao.pageAssets(
                        type,
                        symbol,
                        search,
                        safePage,
                        safeSize,
                        SqlSort.normalizeSortParams(sort));

        List<AssetResponse> responses = new ArrayList<>();
        for (Asset asset : assetPage.content()) {
            responses.add(AssetMapper.toResponse(asset));
        }

        return new Page<>(
                assetPage.page(),
                assetPage.size(),
                assetPage.totalElements(),
                assetPage.totalPages(),
                assetPage.first(),
                assetPage.last(),
                responses);
    }

    /** Updates catalog pricing and logs the change into historical analytics storage. */
    public AssetResponse updatePrice(long assetId, AssetPriceUpdateRequest req) {
        Asset existing = assetDao.findById(assetId);
        if (existing == null) {
            throw new AssetNotFoundException(ASSET_NOT_FOUND);
        }
        assetDao.updateCurrentPrice(assetId, req.price());
        assetPriceHistoryDao.insert(assetId, req.price());
        return AssetMapper.toResponse(assetDao.findById(assetId));
    }

    /** Returns paginated {@link AssetPricePoint} rows after verifying the asset exists. */
    public Page<AssetPricePoint> priceHistory(long assetId, int page, int size) {
        if (assetDao.findById(assetId) == null) {
            throw new AssetNotFoundException(ASSET_NOT_FOUND);
        }
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        return assetPriceHistoryDao.page(assetId, safePage, safeSize);
    }
}
