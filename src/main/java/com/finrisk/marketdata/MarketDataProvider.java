package com.finrisk.marketdata;

import java.util.List;

public interface MarketDataProvider {

    /**
     * Returns up to {@code days} daily closing prices for {@code symbol},
     * newest first. Returns an empty list if the symbol is unknown, the
     * provider is misconfigured, or a network error occurs.
     */
    List<PricePoint> fetchDailyPrices(String symbol, int days);

    /**
     * Returns company profile for {@code symbol}. Never throws; returns a
     * default profile with sector/industry = "Unknown" on any error.
     */
    CompanyProfile fetchProfile(String symbol);

    /**
     * Returns the constituents of ETF {@code etfSymbol} with weights as
     * fractions. Never throws; returns an empty list on unknown symbol,
     * missing key or network error.
     */
    List<EtfHolding> fetchEtfHoldings(String etfSymbol);
}
