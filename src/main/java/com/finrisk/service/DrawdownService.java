package com.finrisk.service;

import com.finrisk.dto.response.DrawdownResponse;
import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.HoldingDrawdown;
import com.finrisk.util.ReturnsMath;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DrawdownService {

    private final PortfolioPrices prices;

    public DrawdownService(PortfolioPrices prices) {
        this.prices = prices;
    }

    public DrawdownResponse computeDrawdown(long accountId) {
        List<Holding> holdings = prices.requireHoldings(accountId);

        double[][] returns = new double[holdings.size()][];
        List<HoldingDrawdown> perHolding = new ArrayList<>();
        for (int i = 0; i < returns.length; i++) {
            Holding h = holdings.get(i);
            List<Double> series = prices.assetPricesOldestFirst(h.assetId());
            returns[i] = ReturnsMath.logReturns(series);
            perHolding.add(new HoldingDrawdown(h.assetId(), h.symbol(), maxDrawdown(series)));
        }

        Double max = null, current = null;
        if (!holdings.isEmpty()) {
            double[] r = ReturnsMath.weightedSum(returns, prices.weights(holdings));
            if (r.length >= PortfolioPrices.MIN_SAMPLES) {
                double index = 1.0, peak = 1.0, min = 0.0, last = 0.0;
                for (double x : r) {
                    index *= Math.exp(x);
                    peak = Math.max(peak, index);
                    last = index / peak - 1;
                    min = Math.min(min, last);
                }
                max = min;
                current = last;
            }
        }
        return new DrawdownResponse(accountId, max, current, perHolding);
    }

    private static Double maxDrawdown(List<Double> prices) {
        if (prices.size() < 2) return null;
        double peak = prices.get(0), min = 0.0;
        for (double p : prices) {
            peak = Math.max(peak, p);
            min = Math.min(min, p / peak - 1);
        }
        return min;
    }
}
