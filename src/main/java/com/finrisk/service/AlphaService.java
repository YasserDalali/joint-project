package com.finrisk.service;

import com.finrisk.dto.response.AlphaResponse;
import com.finrisk.dto.response.Holding;
import com.finrisk.util.ReturnsMath;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlphaService {

    private static final int TRADING_DAYS = 252;

    private final PortfolioPrices prices;
    private final BetaService betaService;

    public AlphaService(PortfolioPrices prices, BetaService betaService) {
        this.prices = prices;
        this.betaService = betaService;
    }

    public AlphaResponse computeAlpha(long accountId, double riskFree) {
        List<Holding> holdings = prices.requireHoldings(accountId);
        double beta = betaService.computeBeta(accountId).portfolioBeta();

        Double alpha = null, portfolioReturn = null, benchmarkReturn = null;
        if (!holdings.isEmpty()) {
            double[][] returns = new double[holdings.size()][];
            for (int i = 0; i < returns.length; i++) {
                returns[i] = ReturnsMath.logReturns(prices.assetPricesOldestFirst(holdings.get(i).assetId()));
            }
            double[] p = ReturnsMath.weightedSum(returns, prices.weights(holdings));
            if (p.length >= PortfolioPrices.MIN_SAMPLES) {
                double[] b = ReturnsMath.logReturns(prices.benchmarkPricesOldestFirst());
                if (b.length >= PortfolioPrices.MIN_SAMPLES) {
                    portfolioReturn = ReturnsMath.mean(p) * TRADING_DAYS;
                    benchmarkReturn = ReturnsMath.mean(b) * TRADING_DAYS;
                    alpha = portfolioReturn - (riskFree + beta * (benchmarkReturn - riskFree));
                }
            }
        }
        return new AlphaResponse(accountId, alpha, portfolioReturn, benchmarkReturn, beta, riskFree,
                PortfolioPrices.BENCHMARK);
    }
}
