package com.finrisk.service;

import com.finrisk.dao.AssetPriceHistoryDao;
import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.Holding;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.marketdata.MarketDataProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PortfolioPrices {

    public static final String BENCHMARK = "SPY";
    public static final int PRICE_DAYS = 90;
    public static final int MIN_SAMPLES = 5;

    private final PortfolioDao portfolioDao;
    private final AssetPriceHistoryDao priceHistoryDao;
    private final MarketDataProvider marketDataProvider;

    public PortfolioPrices(
            PortfolioDao portfolioDao,
            AssetPriceHistoryDao priceHistoryDao,
            MarketDataProvider marketDataProvider) {
        this.portfolioDao = portfolioDao;
        this.priceHistoryDao = priceHistoryDao;
        this.marketDataProvider = marketDataProvider;
    }

    public List<Holding> requireHoldings(long accountId) {
        if (portfolioDao.cashBalance(accountId).isEmpty()) {
            throw new AccountNotFoundException("Account not found");
        }
        return portfolioDao.holdings(accountId);
    }

    public double[] weights(List<Holding> holdings) {
        BigDecimal total = holdings.stream()
                .map(Holding::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double[] w = new double[holdings.size()];
        if (total.signum() == 0) return w;
        for (int i = 0; i < w.length; i++) {
            w[i] = holdings.get(i).currentValue().divide(total, 8, RoundingMode.HALF_UP).doubleValue();
        }
        return w;
    }

    public List<Double> assetPricesOldestFirst(long assetId) {
        return priceHistoryDao.latestPrices(assetId, PRICE_DAYS).stream()
                .map(BigDecimal::doubleValue).toList().reversed();
    }

    public List<Double> benchmarkPricesOldestFirst() {
        return marketDataProvider.fetchDailyPrices(BENCHMARK, PRICE_DAYS).stream()
                .map(p -> p.close().doubleValue()).toList().reversed();
    }
}
