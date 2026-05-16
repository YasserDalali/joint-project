package com.finrisk.controller;

import com.finrisk.dto.response.PortfolioResponse;
import com.finrisk.dto.response.ProfitLossResponse;
import com.finrisk.dto.response.RiskScoreResponse;
import com.finrisk.service.PortfolioService;
import com.finrisk.service.ProfitLossService;
import com.finrisk.service.RiskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aggregated analytics routes scoped under an {@code accountId} path prefix. */
@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final ProfitLossService profitLossService;
    private final RiskService riskService;

    /** Collects services responsible for holdings snapshots, P&amp;L, and Strategy-driven risk scoring. */
    public PortfolioController(
            PortfolioService portfolioService,
            ProfitLossService profitLossService,
            RiskService riskService) {
        this.portfolioService = portfolioService;
        this.profitLossService = profitLossService;
        this.riskService = riskService;
    }

    /** Returns combined cash + holdings valuations for dashboard summaries. */
    @GetMapping("/portfolio")
    public PortfolioResponse portfolio(@PathVariable long accountId) {
        return portfolioService.getPortfolio(accountId);
    }

    /** Exposes profit rows aggregated per holding within an account. */
    @GetMapping("/profit-loss")
    public ProfitLossResponse profitLoss(@PathVariable long accountId) {
        return profitLossService.getProfitLoss(accountId);
    }

    /** Computes numeric risk scores plus qualitative breakdown entries. */
    @GetMapping("/risk")
    public RiskScoreResponse risk(@PathVariable long accountId) {
        return riskService.computeRisk(accountId);
    }
}
