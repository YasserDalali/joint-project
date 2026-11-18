package com.finrisk.controller;

import com.finrisk.dto.response.AlphaResponse;
import com.finrisk.dto.response.BetaResponse;
import com.finrisk.dto.response.CorrelationResponse;
import com.finrisk.dto.response.DownsideResponse;
import com.finrisk.dto.response.DrawdownResponse;
import com.finrisk.dto.response.LookThroughResponse;
import com.finrisk.dto.response.PortfolioResponse;
import com.finrisk.dto.response.ProfitLossResponse;
import com.finrisk.dto.response.RiskScoreResponse;
import com.finrisk.dto.response.DiversificationResponse;
import com.finrisk.dto.response.SectorExposureResponse;
import com.finrisk.service.AlphaService;
import com.finrisk.service.BetaService;
import com.finrisk.service.CorrelationService;
import com.finrisk.service.DiversificationService;
import com.finrisk.service.DownsideService;
import com.finrisk.service.DrawdownService;
import com.finrisk.service.EtfLookThroughService;
import com.finrisk.service.PortfolioService;
import com.finrisk.service.ProfitLossService;
import com.finrisk.service.RiskService;
import com.finrisk.service.SectorExposureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final AlphaService alphaService;
    private final DownsideService downsideService;
    private final EtfLookThroughService etfLookThroughService;

    public PortfolioController(
            PortfolioService portfolioService,
            ProfitLossService profitLossService,
            RiskService riskService,
            BetaService betaService,
            SectorExposureService sectorExposureService,
            DiversificationService diversificationService,
            CorrelationService correlationService,
            DrawdownService drawdownService,
            AlphaService alphaService,
            DownsideService downsideService,
            EtfLookThroughService etfLookThroughService) {
        this.portfolioService = portfolioService;
        this.profitLossService = profitLossService;
        this.riskService = riskService;
        this.betaService = betaService;
        this.sectorExposureService = sectorExposureService;
        this.diversificationService = diversificationService;
        this.correlationService = correlationService;
        this.drawdownService = drawdownService;
        this.alphaService = alphaService;
        this.downsideService = downsideService;
        this.etfLookThroughService = etfLookThroughService;
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

    @GetMapping("/alpha")
    public AlphaResponse alpha(
            @PathVariable long accountId, @RequestParam(defaultValue = "0.0") double riskFree) {
        return alphaService.computeAlpha(accountId, riskFree);
    }

    @GetMapping("/downside")
    public DownsideResponse downside(@PathVariable long accountId) {
        return downsideService.computeDownside(accountId);
    }

    @GetMapping("/etf-look-through")
    public LookThroughResponse etfLookThrough(@PathVariable long accountId) {
        return etfLookThroughService.lookThrough(accountId);
    }
}
