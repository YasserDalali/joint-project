package com.finrisk.config;

import com.finrisk.strategy.risk.RiskCalculationStrategy;
import com.finrisk.strategy.risk.VolatilityRiskStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StrategyConfig {

    @Bean
    public RiskCalculationStrategy riskCalculationStrategy() {
        return new VolatilityRiskStrategy(5);
    }
}
