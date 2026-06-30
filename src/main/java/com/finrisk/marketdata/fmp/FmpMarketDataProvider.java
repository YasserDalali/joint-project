package com.finrisk.marketdata.fmp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finrisk.marketdata.MarketDataProvider;
import com.finrisk.marketdata.PricePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class FmpMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(FmpMarketDataProvider.class);

    private final FmpConfig config;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public FmpMarketDataProvider(FmpConfig config) {
        this.config = config;
        this.http = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    @Override
    public List<PricePoint> fetchDailyPrices(String symbol, int days) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return List.of();
        }
        try {
            String from = LocalDate.now().minusDays(days).toString();
            String url = "%s/historical-price-full/%s?from=%s&apikey=%s"
                    .formatted(config.getBaseUrl(), symbol, from, config.getApiKey());
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .build();
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
            log.warn("FMP fetch failed for symbol {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }
}
