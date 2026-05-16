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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Combines portfolio holdings with historical prices using an injected {@link RiskCalculationStrategy}. */
@Service
public class RiskService {

    private final PortfolioDao portfolioDao;
    private final AssetDao assetDao;
    private final AssetPriceHistoryDao assetPriceHistoryDao;
    private final RiskCalculationStrategy strategy;

    /** Supplies DAO readers plus the Strategy bean configured in {@link com.finrisk.config.StrategyConfig}. */
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

    /** Computes a weighted volatility score, qualitative level, and per-asset breakdown rows. */
    public RiskScoreResponse computeRisk(long accountId) {
        if (portfolioDao.cashBalance(accountId).isEmpty()) {
            throw new AccountNotFoundException("Account not found");
        }

        List<Holding> holdings = portfolioDao.holdings(accountId);
        BigDecimal totalValue = BigDecimal.ZERO;
        for (Holding holding : holdings) {
            totalValue = totalValue.add(holding.currentValue());
        }

        int minSamples = 5;
        if (strategy instanceof VolatilityRiskStrategy volatilityStrategy) {
            minSamples = volatilityStrategy.minSamples();
        }

        if (totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return new RiskScoreResponse(accountId, 0.0, RiskLevel.LOW, List.of());
        }

        List<RiskBreakdownItem> breakdown = new ArrayList<>();
        double weightedSigma = 0.0;

        for (Holding holding : holdings) {
            Asset asset = assetDao.findById(holding.assetId());
            List<BigDecimal> newestFirst = assetPriceHistoryDao.latestPrices(holding.assetId(), 30);
            List<BigDecimal> chronologicalAscending = reverseList(newestFirst);

            double weight =
                    holding.currentValue()
                            .divide(totalValue, 8, RoundingMode.HALF_UP)
                            .doubleValue();

            double sigma = Double.NaN;
            RiskLevel level;
            int sampleSize = chronologicalAscending.size();
            BigDecimal volatilityField;

            if (chronologicalAscending.size() >= minSamples && chronologicalAscending.size() >= 2) {
                sigma = strategy.sigmaFromPrices(chronologicalAscending);
                if (!Double.isNaN(sigma)) {
                    level = strategy.levelForSigma(sigma);
                    volatilityField = BigDecimal.valueOf(sigma);
                } else {
                    level = strategy.fallbackLevel(asset);
                    volatilityField = null;
                }
            } else {
                level = strategy.fallbackLevel(asset);
                volatilityField = null;
            }

            breakdown.add(
                    new RiskBreakdownItem(
                            holding.assetId(),
                            holding.symbol(),
                            level,
                            weight,
                            volatilityField,
                            sampleSize));

            if (!Double.isNaN(sigma)) {
                weightedSigma += weight * sigma;
            } else {
                weightedSigma += weight * sigmaFallback(level);
            }
        }

        RiskLevel portfolioLevel = strategy.levelForSigma(weightedSigma);
        double score01to100 = Math.min(100.0, (weightedSigma / 0.12) * 100.0);

        return new RiskScoreResponse(accountId, score01to100, portfolioLevel, breakdown);
    }

    /** Reorders newest-first DAO samples into chronological ascending series expected by volatility math. */
    private static List<BigDecimal> reverseList(List<BigDecimal> newestFirst) {
        List<BigDecimal> oldestFirst = new ArrayList<>(newestFirst);
        Collections.reverse(oldestFirst);
        return oldestFirst;
    }

    /** Supplies numeric stand-ins when historical sigma cannot be computed but weights still matter. */
    private static double sigmaFallback(RiskLevel level) {
        if (level == RiskLevel.LOW) {
            return 0.005;
        }
        if (level == RiskLevel.MEDIUM) {
            return 0.02;
        }
        if (level == RiskLevel.HIGH) {
            return 0.045;
        }
        if (level == RiskLevel.VERY_HIGH) {
            return 0.08;
        }
        return 0.02;
    }
}
