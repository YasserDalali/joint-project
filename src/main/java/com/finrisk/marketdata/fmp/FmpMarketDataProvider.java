package com.finrisk.marketdata.fmp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finrisk.marketdata.CompanyProfile;
import com.finrisk.marketdata.EtfHolding;
import com.finrisk.marketdata.MarketDataProvider;
import com.finrisk.marketdata.PricePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class FmpMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(FmpMarketDataProvider.class);

    // FMP v3 etf-holder response fields; weightPercentage is a percent (7.1 = 7.1%).
    private static final String ETF_ASSET_FIELD = "asset";
    private static final String ETF_NAME_FIELD = "name";
    private static final String ETF_WEIGHT_FIELD = "weightPercentage";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final FmpConfig config;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final Duration requestTimeout;

    @Autowired
    public FmpMarketDataProvider(FmpConfig config) {
        this(config, CONNECT_TIMEOUT, REQUEST_TIMEOUT);
    }

    FmpMarketDataProvider(FmpConfig config, Duration connectTimeout, Duration requestTimeout) {
        this.config = config;
        this.http = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        this.mapper = new ObjectMapper();
        this.requestTimeout = requestTimeout;
    }

    /** Path-segment encoding: URLEncoder is form encoding, so '+' becomes %20. */
    private static String enc(String symbol) {
        return URLEncoder.encode(symbol, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private HttpRequest get(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(requestTimeout).GET().build();
    }

    @Override
    public List<PricePoint> fetchDailyPrices(String symbol, int days) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return List.of();
        }
        try {
            String from = LocalDate.now().minusDays(days).toString();
            String url = "%s/historical-price-full/%s?from=%s&apikey=%s"
                    .formatted(config.getBaseUrl(), enc(symbol), from, config.getApiKey());
            HttpRequest req = get(url);
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("FMP returned HTTP {} for symbol {}", resp.statusCode(), symbol);
                return List.of();
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode historical = root.path("historical");
            List<PricePoint> points = new ArrayList<>();
            for (JsonNode node : historical) {
                LocalDate date = LocalDate.parse(node.get("date").asText());
                BigDecimal close = new BigDecimal(node.get("close").asText());
                points.add(new PricePoint(date, close));
            }
            return points;
        } catch (Exception e) {
            log.warn("FMP fetch failed for symbol {}: {}", symbol, e.getClass().getSimpleName());
            return List.of();
        }
    }

    @Override
    public CompanyProfile fetchProfile(String symbol) {
        CompanyProfile unknown = new CompanyProfile(symbol, "Unknown", "Unknown", symbol);
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return unknown;
        }
        try {
            String url = "%s/profile/%s?apikey=%s"
                    .formatted(config.getBaseUrl(), enc(symbol), config.getApiKey());
            HttpRequest req = get(url);
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("FMP profile returned HTTP {} for symbol {}", resp.statusCode(), symbol);
                return unknown;
            }
            JsonNode root = mapper.readTree(resp.body());
            if (!root.isArray() || root.isEmpty()) {
                return unknown;
            }
            JsonNode p = root.get(0);
            return new CompanyProfile(
                    symbol,
                    p.path("sector").asText("Unknown"),
                    p.path("industry").asText("Unknown"),
                    p.path("companyName").asText(symbol));
        } catch (Exception e) {
            log.warn("FMP fetchProfile failed for symbol {}: {}", symbol, e.getClass().getSimpleName());
            return unknown;
        }
    }

    @Override
    public List<EtfHolding> fetchEtfHoldings(String etfSymbol) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return List.of();
        }
        try {
            String url = "%s/etf-holder/%s?apikey=%s"
                    .formatted(config.getBaseUrl(), enc(etfSymbol), config.getApiKey());
            HttpRequest req = get(url);
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("FMP etf-holder returned HTTP {} for symbol {}", resp.statusCode(), etfSymbol);
                return List.of();
            }
            JsonNode root = mapper.readTree(resp.body());
            if (!root.isArray()) {
                return List.of();
            }
            List<EtfHolding> holdings = new ArrayList<>();
            for (JsonNode n : root) {
                String asset = n.path(ETF_ASSET_FIELD).asText("").trim();
                if (asset.isEmpty() || !n.hasNonNull(ETF_WEIGHT_FIELD)) continue;
                try {
                    BigDecimal weight = new BigDecimal(n.get(ETF_WEIGHT_FIELD).asText()).movePointLeft(2);
                    holdings.add(new EtfHolding(asset, n.path(ETF_NAME_FIELD).asText(asset), weight));
                } catch (NumberFormatException skip) {
                    // bad weight: skip this row only
                }
            }
            return holdings;
        } catch (Exception e) {
            log.warn("FMP fetchEtfHoldings failed for symbol {}: {}", etfSymbol, e.getClass().getSimpleName());
            return List.of();
        }
    }
}
