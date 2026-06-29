package com.finrisk.marketdata.fmp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FmpConfig {

    @Value("${fmp.api-key:}")
    public String apiKey;

    @Value("${fmp.base-url:https://financialmodelingprep.com/api/v3}")
    public String baseUrl;
}
