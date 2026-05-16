package com.finrisk.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Single observation of an asset price used when charting historical volatility or valuations. */
public record AssetPricePoint(BigDecimal price, String currency, LocalDateTime recordedAt) {

    /** Convenience overload defaulting the denomination to USD for teaching datasets. */
    public AssetPricePoint(BigDecimal price, LocalDateTime recordedAt) {
        this(price, "USD", recordedAt);
    }
}
