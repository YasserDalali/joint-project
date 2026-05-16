package com.finrisk.config;

import com.finrisk.strategy.risk.RiskCalculationStrategy;
import com.finrisk.strategy.risk.VolatilityRiskStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring wiring for Strategy-pattern collaborators such as portfolio risk calculators. */
@Configuration
public class StrategyConfig {

    /** Exposes the concrete {@link RiskCalculationStrategy} bean injected into services. */
    @Bean
    public RiskCalculationStrategy riskCalculationStrategy() {
        return new VolatilityRiskStrategy(5);
    }
}
