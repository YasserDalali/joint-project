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

import java.util.List;

@Service
public class AssetService {

    private final AssetDao assetDao;
    private final AssetPriceHistoryDao assetPriceHistoryDao;

    public AssetService(AssetDao assetDao, AssetPriceHistoryDao assetPriceHistoryDao) {
        this.assetDao = assetDao;
        this.assetPriceHistoryDao = assetPriceHistoryDao;
    }

    public AssetResponse createAsset(AssetCreateRequest req) {
        Asset a = AssetFactory.create(req);
        Asset saved = assetDao.save(a);
        assetPriceHistoryDao.insert(saved.id(), saved.currentPrice());
        return AssetMapper.toResponse(assetDao.findById(saved.id()));
    }

    public AssetResponse getAsset(long id) {
        Asset a = assetDao.findById(id);
        if (a == null) {
            throw new AssetNotFoundException("Asset not found");
        }
        return AssetMapper.toResponse(a);
    }

    public Page<AssetResponse> listAssets(
            AssetType type, String symbol, String search, int page, int size, List<String> sort) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<Asset> p =
                assetDao.pageAssets(
                        type,
                        symbol,
                        search,
                        safePage,
                        safeSize,
                        SqlSort.normalizeSortParams(sort));
        return new Page<>(
                p.page(),
                p.size(),
                p.totalElements(),
                p.totalPages(),
                p.first(),
                p.last(),
                p.content().stream().map(AssetMapper::toResponse).toList());
    }

    public AssetResponse updatePrice(long assetId, AssetPriceUpdateRequest req) {
        Asset existing = assetDao.findById(assetId);
        if (existing == null) {
            throw new AssetNotFoundException("Asset not found");
        }
        assetDao.updateCurrentPrice(assetId, req.price());
        assetPriceHistoryDao.insert(assetId, req.price());
        return AssetMapper.toResponse(assetDao.findById(assetId));
    }

    public Page<AssetPricePoint> priceHistory(long assetId, int page, int size) {
        if (assetDao.findById(assetId) == null) {
            throw new AssetNotFoundException("Asset not found");
        }
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        return assetPriceHistoryDao.page(assetId, safePage, safeSize);
    }
}
