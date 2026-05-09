package com.finrisk.service;

import com.finrisk.dao.AssetDao;
import com.finrisk.dao.AssetPriceHistoryDao;
import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.RiskBreakdownItem;
import com.finrisk.dto.response.RiskScoreResponse;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.model.Asset;
import com.finrisk.model.RiskLevel;
import com.finrisk.strategy.risk.RiskCalculationStrategy;
import com.finrisk.strategy.risk.VolatilityRiskStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskService {

    private final PortfolioDao portfolioDao;
    private final AssetDao assetDao;
    private final AssetPriceHistoryDao assetPriceHistoryDao;
    private final RiskCalculationStrategy strategy;

    public RiskService(
            PortfolioDao portfolioDao,
            AssetDao assetDao,
            AssetPriceHistoryDao assetPriceHistoryDao,
            RiskCalculationStrategy strategy) {
        this.portfolioDao = portfolioDao;
        this.assetDao = assetDao;
        this.assetPriceHistoryDao = assetPriceHistoryDao;
        this.strategy = strategy;
    }

    public RiskScoreResponse computeRisk(long accountId) {
        if (portfolioDao.cashBalance(accountId).isEmpty()) {
            throw new AccountNotFoundException("Account not found");
        }
        List<Holding> holdings = portfolioDao.holdings(accountId);
        BigDecimal totalValue =
                holdings.stream().map(Holding::currentValue).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RiskBreakdownItem> breakdown = new ArrayList<>();
        double weightedSigma = 0.0;
        int minSamples = strategy instanceof VolatilityRiskStrategy v ? v.minSamples() : 5;

        if (totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return new RiskScoreResponse(accountId, 0.0, RiskLevel.LOW, List.of());
        }

        for (Holding h : holdings) {
            Asset asset = assetDao.findById(h.assetId());
            List<BigDecimal> newestFirst = assetPriceHistoryDao.latestPrices(h.assetId(), 30);
            List<BigDecimal> chronologicalAscending = newestFirst.reversed();

            double weight =
                    h.currentValue().divide(totalValue, 8, java.math.RoundingMode.HALF_UP).doubleValue();

            double sigma = Double.NaN;
            RiskLevel lvl;
            Integer sampleSize = chronologicalAscending.size();
            BigDecimal volField;
            if (chronologicalAscending.size() >= minSamples && chronologicalAscending.size() >= 2) {
                sigma = strategy.sigmaFromPrices(chronologicalAscending);
                if (!Double.isNaN(sigma)) {
                    lvl = strategy.levelForSigma(sigma);
                    volField = BigDecimal.valueOf(sigma);
                } else {
                    lvl = strategy.fallbackLevel(asset);
                    volField = null;
                }
            } else {
                lvl = strategy.fallbackLevel(asset);
                volField = null;
            }

            breakdown.add(
                    new RiskBreakdownItem(h.assetId(), h.symbol(), lvl, weight, volField, sampleSize));

            if (!Double.isNaN(sigma)) {
                weightedSigma += weight * sigma;
            } else {
                weightedSigma += weight * sigmaFallback(lvl);
            }
        }

        RiskLevel portfolioLevel = strategy.levelForSigma(weightedSigma);
        double score01to100 = Math.min(100.0, (weightedSigma / 0.12) * 100.0);

        return new RiskScoreResponse(accountId, score01to100, portfolioLevel, breakdown);
    }

    private static double sigmaFallback(RiskLevel lvl) {
        return switch (lvl) {
            case LOW -> 0.005;
            case MEDIUM -> 0.02;
            case HIGH -> 0.045;
            case VERY_HIGH -> 0.08;
        };
    }
}
