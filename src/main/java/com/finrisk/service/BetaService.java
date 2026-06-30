package com.finrisk.service;

import com.finrisk.dao.AssetPriceHistoryDao;
import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.BetaResponse;
import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.HoldingBeta;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.marketdata.MarketDataProvider;
import com.finrisk.marketdata.PricePoint;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class BetaService {

    private static final String BENCHMARK = "SPY";
    private static final int PRICE_DAYS = 90;
    private static final int MIN_SAMPLES = 5;

    private final PortfolioDao portfolioDao;
    private final AssetPriceHistoryDao priceHistoryDao;
    private final MarketDataProvider marketDataProvider;

    public BetaService(
            PortfolioDao portfolioDao,
            AssetPriceHistoryDao priceHistoryDao,
            MarketDataProvider marketDataProvider) {
        this.portfolioDao = portfolioDao;
        this.priceHistoryDao = priceHistoryDao;
        this.marketDataProvider = marketDataProvider;
    }

    public BetaResponse computeBeta(long accountId) {
        if (portfolioDao.cashBalance(accountId).isEmpty()) {
            throw new AccountNotFoundException("Account not found");
        }

        List<Holding> holdings = portfolioDao.holdings(accountId);
        if (holdings.isEmpty()) {
            return new BetaResponse(accountId, 1.0, BENCHMARK, List.of());
        }

        List<PricePoint> spyPoints = marketDataProvider.fetchDailyPrices(BENCHMARK, PRICE_DAYS);
        List<Double> spyPrices = spyPoints.stream()
                .map(p -> p.close().doubleValue())
                .toList()
                .reversed();

        BigDecimal totalValue = holdings.stream()
                .map(Holding::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<HoldingBeta> holdingBetas = new ArrayList<>();
        double portfolioBeta = 0.0;

        for (Holding h : holdings) {
            List<Double> assetPrices = priceHistoryDao.latestPrices(h.assetId(), PRICE_DAYS)
                    .stream().map(BigDecimal::doubleValue).toList().reversed();

            double beta = computeHoldingBeta(assetPrices, spyPrices);

            double weight = totalValue.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                    : h.currentValue().divide(totalValue, 8, RoundingMode.HALF_UP).doubleValue();

            holdingBetas.add(new HoldingBeta(h.assetId(), h.symbol(), beta, weight));
            portfolioBeta += weight * beta;
        }

        return new BetaResponse(accountId, portfolioBeta, BENCHMARK, holdingBetas);
    }

    private double computeHoldingBeta(List<Double> assetPrices, List<Double> spyPrices) {
        int n = Math.min(assetPrices.size(), spyPrices.size()) - 1;
        if (n < MIN_SAMPLES) return 1.0;

        double[] rA = logReturns(assetPrices.subList(0, n + 1));
        double[] rM = logReturns(spyPrices.subList(0, n + 1));
        int len = Math.min(rA.length, rM.length);

        double meanA = mean(rA, len);
        double meanM = mean(rM, len);

        double cov = 0.0, varM = 0.0;
        for (int i = 0; i < len; i++) {
            cov  += (rA[i] - meanA) * (rM[i] - meanM);
            varM += (rM[i] - meanM) * (rM[i] - meanM);
        }
        if (len <= 1 || varM == 0.0) return 1.0;
        return (cov / (len - 1)) / (varM / (len - 1));
    }

    private double[] logReturns(List<Double> prices) {
        double[] r = new double[prices.size() - 1];
        for (int i = 1; i < prices.size(); i++) {
            r[i - 1] = Math.log(prices.get(i) / prices.get(i - 1));
        }
        return r;
    }

    private double mean(double[] arr, int len) {
        double sum = 0;
        for (int i = 0; i < len; i++) sum += arr[i];
        return sum / len;
    }
}
