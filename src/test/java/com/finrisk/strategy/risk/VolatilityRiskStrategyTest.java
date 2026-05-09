package com.finrisk.strategy.risk;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VolatilityRiskStrategyTest {

    @Test
    void nearlyFlat_series_maps_low_sigma_bucket() {
        VolatilityRiskStrategy s = new VolatilityRiskStrategy(5);
        List<BigDecimal> asc =
                List.of(
                        bd("100"),
                        bd("100.1"),
                        bd("100.05"),
                        bd("100.2"),
                        bd("100.15"),
                        bd("100.12"));
        double sigma = s.sigmaFromPrices(asc);
        assertThat(sigma).isFinite();
        assertThat(s.levelForSigma(sigma)).isNotNull();
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
