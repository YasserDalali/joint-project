package com.finrisk.controller;

import com.finrisk.dto.request.AssetCreateRequest;
import com.finrisk.dto.request.AssetPriceUpdateRequest;
import com.finrisk.dto.response.AssetPricePoint;
import com.finrisk.dto.response.AssetResponse;
import com.finrisk.dto.response.Page;
import com.finrisk.model.AssetType;
import com.finrisk.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public Page<AssetResponse> list(
            @RequestParam(required = false) AssetType type,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort) {
        return assetService.listAssets(type, symbol, search, page, size, sort == null ? List.of() : sort);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse create(@Valid @RequestBody AssetCreateRequest req) {
        return assetService.createAsset(req);
    }

    @GetMapping("/{id}")
    public AssetResponse get(@PathVariable long id) {
        return assetService.getAsset(id);
    }

    @PutMapping("/{id}/price")
    public AssetResponse updatePrice(@PathVariable long id, @Valid @RequestBody AssetPriceUpdateRequest req) {
        return assetService.updatePrice(id, req);
    }

    @GetMapping("/{id}/price-history")
    public Page<AssetPricePoint> history(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return assetService.priceHistory(id, page, size);
    }
}
