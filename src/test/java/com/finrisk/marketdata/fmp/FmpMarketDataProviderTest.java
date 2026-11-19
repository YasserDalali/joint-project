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

    // ---- hardening tests ----

    private static com.sun.net.httpserver.HttpServer serve(
            java.util.function.Consumer<String> rawPathSink, int status, String body) throws Exception {
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
        server.createContext("/", ex -> {
            rawPathSink.accept(ex.getRequestURI().getRawPath());
            byte[] b = body.getBytes();
            ex.sendResponseHeaders(status, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        server.start();
        return server;
    }

    private static FmpConfig cfgFor(com.sun.net.httpserver.HttpServer server) {
        FmpConfig cfg = new FmpConfig();
        cfg.setApiKey("secret-key-123");
        cfg.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        return cfg;
    }

    private List<EtfHolding> holdingsFor(int status, String body) throws Exception {
        com.sun.net.httpserver.HttpServer server = serve(p -> { }, status, body);
        try {
            return new FmpMarketDataProvider(cfgFor(server)).fetchEtfHoldings("SPY");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void symbolWithSpaceIsPercentEncodedInAllThreeMethods() throws Exception {
        java.util.List<String> paths = new java.util.concurrent.CopyOnWriteArrayList<>();
        com.sun.net.httpserver.HttpServer server = serve(paths::add, 200, "[]");
        try {
            FmpMarketDataProvider p = new FmpMarketDataProvider(cfgFor(server));
            p.fetchDailyPrices("BRK B", 5);
            p.fetchProfile("BRK B");
            p.fetchEtfHoldings("BRK B");
            assertThat(paths).containsExactly(
                    "/historical-price-full/BRK%20B", "/profile/BRK%20B", "/etf-holder/BRK%20B");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void pathTraversalSymbolStaysOneEncodedSegment() throws Exception {
        java.util.List<String> paths = new java.util.concurrent.CopyOnWriteArrayList<>();
        com.sun.net.httpserver.HttpServer server = serve(paths::add, 200, "[]");
        try {
            new FmpMarketDataProvider(cfgFor(server)).fetchEtfHoldings("../x");
            assertThat(paths).containsExactly("/etf-holder/..%2Fx");
        } finally {
            server.stop(0);
        }
    }

    /** A port that was just bound and released, so nothing is listening on it. */
    private static int closedPort() throws Exception {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    @Test
    void interruptFlagIsRestoredInAllThreeMethods() throws Exception {
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        server.createContext("/", ex -> {
            try { release.await(); } catch (InterruptedException ignored) { }
            ex.close();
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        var logs = captureLogs();
        try {
            FmpMarketDataProvider p = new FmpMarketDataProvider(cfgFor(server));
            java.util.List<java.util.function.Supplier<Object>> calls = java.util.List.of(
                    () -> p.fetchDailyPrices("AAPL", 5),
                    () -> p.fetchProfile("AAPL"),
                    () -> p.fetchEtfHoldings("AAPL"));
            for (var call : calls) {
                Thread.currentThread().interrupt();
                Object result = call.get();
                boolean stillInterrupted = Thread.interrupted(); // also clears it
                assertThat(stillInterrupted).as("interrupt flag").isTrue();
                if (result instanceof List<?> l) {
                    assertThat(l).isEmpty();
                } else {
                    assertThat(((CompanyProfile) result).sector()).isEqualTo("Unknown");
                }
            }
            assertThat(logs.list).hasSize(3);
            for (var ev : logs.list) {
                assertThat(ev.getFormattedMessage()).doesNotContain("secret-key-123")
                        .doesNotContain("apikey").doesNotContain("http://").doesNotContain("null")
                        .contains("AAPL");
                assertThat(ev.getThrowableProxy()).isNull();
            }
        } finally {
            Thread.interrupted();
            releaseLogs(logs);
            release.countDown();
            server.stop(0);
        }
    }

    @Test
    void dotSymbolsAreRejectedWithoutARequest() throws Exception {
        java.util.List<String> paths = new java.util.concurrent.CopyOnWriteArrayList<>();
        com.sun.net.httpserver.HttpServer server = serve(paths::add, 200, "[]");
        try {
            FmpMarketDataProvider p = new FmpMarketDataProvider(cfgFor(server));
            for (String sym : List.of(".", "..")) {
                assertThat(p.fetchDailyPrices(sym, 5)).isEmpty();
                assertThat(p.fetchProfile(sym).sector()).isEqualTo("Unknown");
                assertThat(p.fetchEtfHoldings(sym)).isEmpty();
            }
            assertThat(paths).isEmpty();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void dotInsideSymbolIsStillRequested() throws Exception {
        java.util.List<String> paths = new java.util.concurrent.CopyOnWriteArrayList<>();
        com.sun.net.httpserver.HttpServer server = serve(paths::add, 200, "[]");
        try {
            new FmpMarketDataProvider(cfgFor(server)).fetchEtfHoldings("BRK.B");
            assertThat(paths).containsExactly("/etf-holder/BRK.B");
        } finally {
            server.stop(0);
        }
    }

    private static ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> captureLogs() {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(FmpMarketDataProvider.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void releaseLogs(ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> a) {
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(FmpMarketDataProvider.class))
                .detachAppender(a);
    }

    @Test
    void failedRequestLogsNoApiKeyNoUrlNoNull() throws Exception {
        var logs = captureLogs();
        try {
            FmpConfig cfg = new FmpConfig();
            cfg.setApiKey("secret-key-123");
            cfg.setBaseUrl("http://localhost:" + closedPort());
            FmpMarketDataProvider p = new FmpMarketDataProvider(cfg);
            p.fetchDailyPrices("AAPL", 5);
            p.fetchProfile("AAPL");
            p.fetchEtfHoldings("AAPL");

            assertThat(logs.list).hasSize(3);
            for (var ev : logs.list) {
                String msg = ev.getFormattedMessage();
                assertThat(msg).doesNotContain("secret-key-123").doesNotContain("apikey")
                        .doesNotContain("http://").doesNotContain("null").contains("AAPL");
                assertThat(ev.getThrowableProxy()).isNull();
            }
        } finally {
            releaseLogs(logs);
        }
    }

    @Test
    void non200LogsNoApiKeyOrUrl() throws Exception {
        var logs = captureLogs();
        com.sun.net.httpserver.HttpServer server = serve(p -> { }, 500, "boom");
        try {
            FmpMarketDataProvider p = new FmpMarketDataProvider(cfgFor(server));
            p.fetchDailyPrices("AAPL", 5);
            p.fetchProfile("AAPL");
            p.fetchEtfHoldings("AAPL");
            assertThat(logs.list).hasSize(3);
            for (var ev : logs.list) {
                assertThat(ev.getFormattedMessage()).doesNotContain("secret-key-123").doesNotContain("apikey");
            }
        } finally {
            releaseLogs(logs);
            server.stop(0);
        }
    }

    @Test
    void etfHoldings_non200ReturnsEmpty() throws Exception {
        assertThat(holdingsFor(500, "[{\"asset\":\"AAPL\",\"weightPercentage\":7.1}]")).isEmpty();
    }

    @Test
    void etfHoldings_malformedJsonReturnsEmpty() throws Exception {
        assertThat(holdingsFor(200, "{not json")).isEmpty();
    }

    @Test
    void etfHoldings_nonArrayBodyReturnsEmpty() throws Exception {
        assertThat(holdingsFor(200, "{\"data\":{\"asset\":\"AAPL\",\"weightPercentage\":7.1}}")).isEmpty();
    }

    @Test
    void etfHoldings_skipsBadRowsKeepsGoodOnes() throws Exception {
        String body = "[{\"name\":\"no asset\",\"weightPercentage\":1},"
                + "{\"asset\":\" \",\"weightPercentage\":1},"
                + "{\"asset\":\"NULLW\",\"weightPercentage\":null},"
                + "{\"asset\":\"NOW\"},"
                + "{\"asset\":\"ABC\",\"weightPercentage\":\"abc\"},"
                + "{\"asset\":\"AAPL\",\"weightPercentage\":7.1},"
                + "{\"asset\":\"MSFT\",\"weightPercentage\":6.25}]";
        List<EtfHolding> r = holdingsFor(200, body);
        assertThat(r).extracting(EtfHolding::symbol).containsExactly("AAPL", "MSFT");
        assertThat(r.get(0).weight()).isEqualByComparingTo("0.071");
    }

    @Test
    @org.junit.jupiter.api.Timeout(10)
    void requestTimeoutCutsOffServerThatNeverResponds() throws Exception {
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        server.createContext("/", ex -> {
            try { release.await(); } catch (InterruptedException ignored) { }
            ex.close();
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        try {
            FmpMarketDataProvider p = new FmpMarketDataProvider(cfgFor(server),
                    java.time.Duration.ofSeconds(1), java.time.Duration.ofMillis(300));
            long t0 = System.nanoTime();
            assertThat(p.fetchEtfHoldings("SPY")).isEmpty();
            assertThat((System.nanoTime() - t0) / 1_000_000).isLessThan(3000);
        } finally {
            release.countDown();
            server.stop(0);
        }
    }
}
