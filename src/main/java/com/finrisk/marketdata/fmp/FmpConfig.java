package com.finrisk.marketdata.fmp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FmpConfig {

    @Value("${fmp.api-key:}")
    private String apiKey;

    @Value("${fmp.base-url:https://financialmodelingprep.com/api/v3}")
    private String baseUrl;

    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }

    void setApiKey(String apiKey) { this.apiKey = apiKey; }
    void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
