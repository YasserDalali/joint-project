package com.finrisk.marketdata;

import java.math.BigDecimal;

/** One constituent of an ETF; {@code weight} is a fraction (0.071 = 7.1%). */
public record EtfHolding(String symbol, String name, BigDecimal weight) {}
