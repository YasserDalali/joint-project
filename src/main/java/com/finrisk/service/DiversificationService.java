package com.finrisk.service;

import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.AssetTypeAllocation;
import com.finrisk.dto.response.DiversificationResponse;
import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.SectorExposureResponse;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.model.AssetType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class DiversificationService {

    private final PortfolioDao portfolioDao;
    private final SectorExposureService sectorExposureService;

    public DiversificationService(PortfolioDao portfolioDao, SectorExposureService sectorExposureService) {
        this.portfolioDao = portfolioDao;
        this.sectorExposureService = sectorExposureService;
    }

    public DiversificationResponse computeDiversification(long accountId) {
        if (portfolioDao.cashBalance(accountId).isEmpty()) {
            throw new AccountNotFoundException("Account not found");
        }

        List<Holding> holdings = portfolioDao.holdings(accountId);

        if (holdings.isEmpty()) {
            return new DiversificationResponse(accountId, 0.0, 1.0, 0, 0, 0, List.of());
        }

        BigDecimal totalValue = holdings.stream()
                .map(Holding::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double hhi = 0.0;
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            for (Holding h : holdings) {
                double w = h.currentValue().divide(totalValue, 8, RoundingMode.HALF_UP).doubleValue();
                hhi += w * w;
            }
        } else {
            hhi = 1.0;
        }

        double score = BigDecimal.valueOf((1.0 - hhi) * 100.0)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        Map<AssetType, BigDecimal> typeValue = new LinkedHashMap<>();
        for (Holding h : holdings) {
            typeValue.merge(h.assetType(), h.currentValue(), BigDecimal::add);
        }

        List<AssetTypeAllocation> breakdown = typeValue.entrySet().stream()
                .map(e -> {
                    double w = totalValue.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                            : e.getValue().divide(totalValue, 8, RoundingMode.HALF_UP).doubleValue();
                    return new AssetTypeAllocation(e.getKey().name(), w, e.getValue());
                })
                .sorted(Comparator.comparingDouble(AssetTypeAllocation::weight).reversed())
                .toList();

        SectorExposureResponse sectorExposure = sectorExposureService.computeSectorExposure(accountId);
        int sectorCount = sectorExposure.sectors().size();

        return new DiversificationResponse(
                accountId,
                score,
                BigDecimal.valueOf(hhi).setScale(6, RoundingMode.HALF_UP).doubleValue(),
                holdings.size(),
                sectorCount,
                breakdown.size(),
                breakdown);
    }
}
