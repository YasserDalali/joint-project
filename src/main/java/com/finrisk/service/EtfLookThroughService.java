package com.finrisk.service;

import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.LookThroughResponse;
import com.finrisk.dto.response.LookThroughResponse.Exposure;
import com.finrisk.dto.response.LookThroughResponse.Source;
import com.finrisk.marketdata.EtfHolding;
import com.finrisk.marketdata.MarketDataProvider;
import com.finrisk.model.AssetType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EtfLookThroughService {

    private final PortfolioPrices prices;
    private final MarketDataProvider marketData;

    public EtfLookThroughService(PortfolioPrices prices, MarketDataProvider marketData) {
        this.prices = prices;
        this.marketData = marketData;
    }

    public LookThroughResponse lookThrough(long accountId) {
        List<Holding> holdings = prices.requireHoldings(accountId);
        double[] w = prices.weights(holdings);
        Map<String, List<EtfHolding>> etfs = fetchEtfHoldings(holdings);

        Map<String, Acc> acc = new LinkedHashMap<>();
        List<String> unresolved = new ArrayList<>();
        for (int i = 0; i < holdings.size(); i++) {
            Holding h = holdings.get(i);
            List<EtfHolding> constituents = h.assetType() == AssetType.ETF ? etfs.get(h.symbol()) : null;
            if (constituents == null || constituents.isEmpty()) {
                if (constituents != null && !unresolved.contains(h.symbol())) unresolved.add(h.symbol());
                acc.computeIfAbsent(h.symbol(), k -> new Acc(h.name())).direct += w[i];
                continue;
            }
            for (EtfHolding u : constituents) {
                double via = w[i] * u.weight().doubleValue();
                Acc a = acc.computeIfAbsent(u.symbol(), k -> new Acc(u.name()));
                a.via += via;
                a.sources.add(new Source(h.symbol(), via));
            }
        }
        List<Exposure> exposures = acc.entrySet().stream()
                .map(e -> new Exposure(e.getKey(), e.getValue().name, e.getValue().direct + e.getValue().via,
                        e.getValue().direct, e.getValue().via, e.getValue().sources))
                .sorted(Comparator.comparingDouble(Exposure::effectiveWeight).reversed())
                .toList();
        BigDecimal total = holdings.stream().map(Holding::currentValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new LookThroughResponse(accountId, total, exposures, unresolved);
    }

    /** Fetches each distinct ETF's holdings once, keyed by ETF symbol (empty list = unresolved). */
    public Map<String, List<EtfHolding>> fetchEtfHoldings(List<Holding> holdings) {
        Map<String, List<EtfHolding>> out = new LinkedHashMap<>();
        for (Holding h : holdings) {
            if (h.assetType() == AssetType.ETF) {
                out.computeIfAbsent(h.symbol(), marketData::fetchEtfHoldings);
            }
        }
        return out;
    }

    private static final class Acc {
        final String name;
        double direct, via;
        final List<Source> sources = new ArrayList<>();

        Acc(String name) {
            this.name = name;
        }
    }
}
