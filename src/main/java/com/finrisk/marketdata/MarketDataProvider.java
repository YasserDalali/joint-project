package com.finrisk.marketdata;

import java.util.List;

public interface MarketDataProvider {

    /**
     * Returns up to {@code days} daily closing prices for {@code symbol},
     * newest first. Returns an empty list if the symbol is unknown, the
     * provider is misconfigured, or a network error occurs.
     */
    List<PricePoint> fetchDailyPrices(String symbol, int days);
}
