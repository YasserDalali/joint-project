package com.finrisk.service;

import com.finrisk.dto.response.DownsideResponse;
import com.finrisk.dto.response.Holding;
import com.finrisk.util.ReturnsMath;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DownsideService {

    private static final int TRADING_DAYS = 252;

    private final PortfolioPrices prices;

    public DownsideService(PortfolioPrices prices) {
        this.prices = prices;
    }

    public DownsideResponse computeDownside(long accountId) {
        List<Holding> holdings = prices.requireHoldings(accountId);
        Double dd = null, sortino = null, downBeta = null, worst = null;
        if (!holdings.isEmpty()) {
            double[][] returns = new double[holdings.size()][];
            for (int i = 0; i < returns.length; i++) {
                returns[i] = ReturnsMath.logReturns(prices.assetPricesOldestFirst(holdings.get(i).assetId()));
            }
            double[] p = ReturnsMath.weightedSum(returns, prices.weights(holdings));
            if (p.length >= PortfolioPrices.MIN_SAMPLES) {
                double sumSq = 0, min = Double.POSITIVE_INFINITY;
                for (double r : p) {
                    if (r < 0) sumSq += r * r;
                    min = Math.min(min, r);
                }
                dd = Math.sqrt(sumSq / p.length) * Math.sqrt(TRADING_DAYS);
                if (dd > 0) sortino = ReturnsMath.mean(p) * TRADING_DAYS / dd;
                worst = min;
                downBeta = downsideBeta(p, ReturnsMath.logReturns(prices.benchmarkPricesOldestFirst()));
            }
        }
        return new DownsideResponse(accountId, dd, sortino, downBeta, worst, PortfolioPrices.BENCHMARK);
    }

    /** Beta over only the days the benchmark fell; null if fewer than MIN_SAMPLES such days or zero variance. */
    private static Double downsideBeta(double[] p, double[] b) {
        int n = Math.min(p.length, b.length);
        double[] rp = new double[n], rm = new double[n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (b[i] < 0) {
                rp[k] = p[i];
                rm[k++] = b[i];
            }
        }
        if (k < PortfolioPrices.MIN_SAMPLES) return null;
        rp = Arrays.copyOf(rp, k);
        rm = Arrays.copyOf(rm, k);
        double var = ReturnsMath.covariance(rm, rm);
        return var == 0 ? null : ReturnsMath.covariance(rp, rm) / var;
    }
}
