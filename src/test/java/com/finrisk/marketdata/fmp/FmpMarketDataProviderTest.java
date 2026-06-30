package com.finrisk.marketdata.fmp;

import com.finrisk.marketdata.CompanyProfile;
import com.finrisk.marketdata.PricePoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FmpMarketDataProviderTest {

    @Test
    void returnsEmptyWhenApiKeyBlank() {
        FmpConfig cfg = new FmpConfig();
        cfg.setApiKey("");
        cfg.setBaseUrl("https://financialmodelingprep.com/api/v3");

        FmpMarketDataProvider provider = new FmpMarketDataProvider(cfg);
        List<PricePoint> result = provider.fetchDailyPrices("AAPL", 30);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOnNetworkError() {
        FmpConfig cfg = new FmpConfig();
        cfg.setApiKey("test-key");
        cfg.setBaseUrl("http://localhost:19999"); // nothing listening here

        FmpMarketDataProvider provider = new FmpMarketDataProvider(cfg);
        List<PricePoint> result = provider.fetchDailyPrices("AAPL", 30);

        assertThat(result).isEmpty();
    }

    @Test
    void fetchProfile_returnsDefaultWhenApiKeyBlank() {
        FmpConfig cfg = new FmpConfig();
        cfg.setApiKey("");
        cfg.setBaseUrl("https://financialmodelingprep.com/api/v3");

        FmpMarketDataProvider provider = new FmpMarketDataProvider(cfg);
        CompanyProfile profile = provider.fetchProfile("AAPL");

        assertThat(profile.symbol()).isEqualTo("AAPL");
        assertThat(profile.sector()).isEqualTo("Unknown");
    }

    @Test
    void fetchProfile_returnsDefaultOnNetworkError() {
        FmpConfig cfg = new FmpConfig();
        cfg.setApiKey("test-key");
        cfg.setBaseUrl("http://localhost:19999");

        FmpMarketDataProvider provider = new FmpMarketDataProvider(cfg);
        CompanyProfile profile = provider.fetchProfile("AAPL");

        assertThat(profile.symbol()).isEqualTo("AAPL");
        assertThat(profile.sector()).isEqualTo("Unknown");
    }
}
