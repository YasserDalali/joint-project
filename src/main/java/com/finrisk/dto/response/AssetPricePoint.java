package com.finrisk.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssetPricePoint(BigDecimal price, String currency, LocalDateTime recordedAt) {

    public AssetPricePoint(BigDecimal price, LocalDateTime recordedAt) {
        this(price, "USD", recordedAt);
    }
}
