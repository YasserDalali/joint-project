package com.finrisk.service;

import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.LookThroughResponse;
import com.finrisk.dto.response.LookThroughResponse.Exposure;
import com.finrisk.dto.response.LookThroughResponse.Source;
import com.finrisk.dto.response.OverlapResponse;
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

    public OverlapResponse overlap(long accountId) {
        Map<String, List<EtfHolding>> etfs = fetchEtfHoldings(prices.requireHoldings(accountId));
        List<String> unresolved = new ArrayList<>();
        Map<String, Map<String, BigDecimal>> weights = new LinkedHashMap<>();
        etfs.forEach((etf, constituents) -> {
            if (constituents.isEmpty()) {
                unresolved.add(etf);
                return;
            }
            Map<String, BigDecimal> m = new LinkedHashMap<>();
            constituents.forEach(c -> m.merge(c.symbol(), c.weight(), BigDecimal::add));
            weights.put(etf, m);
        });

        List<String> names = new ArrayList<>(weights.keySet());
        List<OverlapResponse.Pair> pairs = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                Map<String, BigDecimal> a = weights.get(names.get(i)), b = weights.get(names.get(j));
                Map<String, BigDecimal> shared = new LinkedHashMap<>();
                a.forEach((sym, wa) -> {
                    if (b.containsKey(sym)) shared.put(sym, wa.min(b.get(sym)));
                });
                double overlap = shared.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
                List<String> symbols = shared.entrySet().stream()
                        .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                        .map(Map.Entry::getKey).toList();
                pairs.add(new OverlapResponse.Pair(names.get(i), names.get(j), overlap, symbols));
            }
        }
        pairs.sort(Comparator.comparingDouble(OverlapResponse.Pair::overlap).reversed());
        return new OverlapResponse(accountId, pairs, unresolved);
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
