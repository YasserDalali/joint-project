package com.finrisk.service;

import com.finrisk.dto.response.CorrelationResponse;
import com.finrisk.dto.response.Holding;
import com.finrisk.util.ReturnsMath;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CorrelationService {

    private final PortfolioPrices prices;

    public CorrelationService(PortfolioPrices prices) {
        this.prices = prices;
    }

    public CorrelationResponse computeCorrelation(long accountId) {
        List<Holding> holdings = prices.requireHoldings(accountId);
        int n = holdings.size();

        double[][] returns = new double[n][];
        for (int i = 0; i < n; i++) {
            returns[i] = ReturnsMath.logReturns(prices.assetPricesOldestFirst(holdings.get(i).assetId()));
        }

        List<List<Double>> matrix = new ArrayList<>();
        double sum = 0;
        int count = 0;
        for (int i = 0; i < n; i++) {
            List<Double> row = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                Double c = i == j ? Double.valueOf(1.0) : correlation(returns[i], returns[j]);
                row.add(c);
                if (j > i && c != null) {
                    sum += c;
                    count++;
                }
            }
            matrix.add(row);
        }

        return new CorrelationResponse(accountId,
                holdings.stream().map(Holding::symbol).toList(),
                matrix,
                count == 0 ? null : sum / count);
    }

    private static Double correlation(double[] a, double[] b) {
        // mean of an empty array is NaN, so guard on sample count first
        if (Math.min(a.length, b.length) < PortfolioPrices.MIN_SAMPLES) return null;
        return ReturnsMath.correlation(a, b);
    }
}
