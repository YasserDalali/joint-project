package com.finrisk.controller;

import com.finrisk.dto.response.BetaResponse;
import com.finrisk.dto.response.CorrelationResponse;
import com.finrisk.dto.response.DrawdownResponse;
import com.finrisk.dto.response.PortfolioResponse;
import com.finrisk.dto.response.ProfitLossResponse;
import com.finrisk.dto.response.RiskScoreResponse;
import com.finrisk.dto.response.DiversificationResponse;
import com.finrisk.dto.response.SectorExposureResponse;
import com.finrisk.service.BetaService;
import com.finrisk.service.CorrelationService;
import com.finrisk.service.DiversificationService;
import com.finrisk.service.DrawdownService;
import com.finrisk.service.PortfolioService;
import com.finrisk.service.ProfitLossService;
import com.finrisk.service.RiskService;
import com.finrisk.service.SectorExposureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final ProfitLossService profitLossService;
    private final RiskService riskService;
    private final BetaService betaService;
    private final SectorExposureService sectorExposureService;
    private final DiversificationService diversificationService;
    private final CorrelationService correlationService;
    private final DrawdownService drawdownService;

    public PortfolioController(
            PortfolioService portfolioService,
            ProfitLossService profitLossService,
            RiskService riskService,
            BetaService betaService,
            SectorExposureService sectorExposureService,
            DiversificationService diversificationService,
            CorrelationService correlationService,
            DrawdownService drawdownService) {
        this.portfolioService = portfolioService;
        this.profitLossService = profitLossService;
        this.riskService = riskService;
        this.betaService = betaService;
        this.sectorExposureService = sectorExposureService;
        this.diversificationService = diversificationService;
        this.correlationService = correlationService;
        this.drawdownService = drawdownService;
    }

    @GetMapping("/portfolio")
    public PortfolioResponse portfolio(@PathVariable long accountId) {
        return portfolioService.getPortfolio(accountId);
    }

    @GetMapping("/profit-loss")
    public ProfitLossResponse profitLoss(@PathVariable long accountId) {
        return profitLossService.getProfitLoss(accountId);
    }

    @GetMapping("/risk")
    public RiskScoreResponse risk(@PathVariable long accountId) {
        return riskService.computeRisk(accountId);
    }

    @GetMapping("/beta")
    public BetaResponse beta(@PathVariable long accountId) {
        return betaService.computeBeta(accountId);
    }

    @GetMapping("/sector-exposure")
    public SectorExposureResponse sectorExposure(@PathVariable long accountId) {
        return sectorExposureService.computeSectorExposure(accountId);
    }

    @GetMapping("/diversification")
    public DiversificationResponse diversification(@PathVariable long accountId) {
        return diversificationService.computeDiversification(accountId);
    }

    @GetMapping("/correlation")
    public CorrelationResponse correlation(@PathVariable long accountId) {
        return correlationService.computeCorrelation(accountId);
    }

    @GetMapping("/drawdown")
    public DrawdownResponse drawdown(@PathVariable long accountId) {
        return drawdownService.computeDrawdown(accountId);
    }
}
