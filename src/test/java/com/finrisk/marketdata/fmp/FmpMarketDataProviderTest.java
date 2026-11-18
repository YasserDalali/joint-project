package com.finrisk.marketdata.fmp;

import com.finrisk.marketdata.CompanyProfile;
import com.finrisk.marketdata.EtfHolding;
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

    @Test
    void fetchEtfHoldings_parsesPercentToFraction() throws Exception {
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
        server.createContext("/etf-holder/SPY", ex -> {
            byte[] body = ("[{\"asset\":\"AAPL\",\"name\":\"Apple Inc\",\"weightPercentage\":7.1},"
                    + "{\"asset\":\"MSFT\",\"name\":\"Microsoft\",\"weightPercentage\":6.25}]").getBytes();
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        try {
            FmpConfig cfg = new FmpConfig();
            cfg.setApiKey("test-key");
            cfg.setBaseUrl("http://localhost:" + server.getAddress().getPort());

            List<EtfHolding> result = new FmpMarketDataProvider(cfg).fetchEtfHoldings("SPY");

            // 7.1 / 100 = 0.071 ; 6.25 / 100 = 0.0625
            assertThat(result).hasSize(2);
            assertThat(result.get(0).symbol()).isEqualTo("AAPL");
            assertThat(result.get(0).name()).isEqualTo("Apple Inc");
            assertThat(result.get(0).weight()).isEqualByComparingTo("0.071");
            assertThat(result.get(1).symbol()).isEqualTo("MSFT");
            assertThat(result.get(1).weight()).isEqualByComparingTo("0.0625");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchEtfHoldings_returnsEmptyOnNetworkError() {
        FmpConfig cfg = new FmpConfig();
        cfg.setApiKey("test-key");
        cfg.setBaseUrl("http://localhost:19999");

        assertThat(new FmpMarketDataProvider(cfg).fetchEtfHoldings("SPY")).isEmpty();
    }

    @Test
    void fetchEtfHoldings_returnsEmptyWhenApiKeyBlank() {
        FmpConfig cfg = new FmpConfig();
        cfg.setApiKey("");
        cfg.setBaseUrl("https://financialmodelingprep.com/api/v3");

        assertThat(new FmpMarketDataProvider(cfg).fetchEtfHoldings("SPY")).isEmpty();
    }
}
