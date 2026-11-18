package com.finrisk.service;

import com.finrisk.dto.response.AlphaResponse;
import com.finrisk.dto.response.BetaResponse;
import com.finrisk.dto.response.Holding;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.model.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlphaServiceTest {

    @Mock PortfolioPrices prices;
    @Mock BetaService betaService;

    AlphaService service;

    @BeforeEach
    void setUp() {
        service = new AlphaService(prices, betaService);
    }

    private static Holding holding(long id, String symbol) {
        return new Holding(id, symbol, symbol, AssetType.STOCK, 1, BigDecimal.TEN, BigDecimal.TEN);
    }

    private void givenBeta(double beta) {
        when(betaService.computeBeta(1L)).thenReturn(new BetaResponse(1L, beta, "SPY", List.of()));
    }

    // 6 prices -> 5 log returns. Mean of log returns telescopes to ln(last/first)/5.
    private static final List<Double> A = List.of(100.0, 130.0, 120.0, 170.0, 150.0, 200.0); // ln2/5
    private static final List<Double> FLAT = List.of(100.0, 100.0, 100.0, 100.0, 100.0, 100.0); // 0
    private static final List<Double> SPY = List.of(100.0, 110.0, 105.0, 140.0, 130.0, 150.0); // ln1.5/5

    private void givenTwoHoldings75_25() {
        Holding a = holding(1, "A"), b = holding(2, "B");
        when(prices.requireHoldings(1L)).thenReturn(List.of(a, b));
        when(prices.weights(List.of(a, b))).thenReturn(new double[] {0.75, 0.25});
        when(prices.assetPricesOldestFirst(1L)).thenReturn(A);
        when(prices.assetPricesOldestFirst(2L)).thenReturn(FLAT);
        when(prices.benchmarkPricesOldestFirst()).thenReturn(SPY);
    }

    @Test
    void alphaMatchesJensenFormulaForWeightedPortfolio() {
        // Portfolio daily mean = 0.75*(ln2/5) + 0.25*0 = 0.75*(ln2/5) = 0.15*ln2 ; annual = 0.15*ln2*252
        // Benchmark annual = ln1.5/5*252. beta 0.8, rf 0.02.
        // alpha = pr - (0.02 + 0.8*(br - 0.02))
        givenTwoHoldings75_25();
        givenBeta(0.8);

        AlphaResponse r = service.computeAlpha(1L, 0.02);

        double pr = 0.75 * Math.log(2) / 5 * 252;
        double br = Math.log(1.5) / 5 * 252;
        assertThat(r.accountId()).isEqualTo(1L);
        assertThat(r.portfolioReturn()).isCloseTo(pr, within(1e-9));
        assertThat(r.benchmarkReturn()).isCloseTo(br, within(1e-9));
        assertThat(r.alpha()).isCloseTo(pr - (0.02 + 0.8 * (br - 0.02)), within(1e-9));
        assertThat(r.beta()).isEqualTo(0.8);
        assertThat(r.riskFree()).isEqualTo(0.02);
        assertThat(r.benchmark()).isEqualTo("SPY");
    }

    @Test
    void riskFreeChangesAlphaAsFormulaSays() {
        // beta 0.5: alpha = pr - rf - 0.5*(br - rf) = pr - 0.5*br - 0.5*rf
        // so going from rf=0 to rf=0.04 lowers alpha by exactly 0.02.
        givenTwoHoldings75_25();
        givenBeta(0.5);

        double a0 = service.computeAlpha(1L, 0.0).alpha();
        double a4 = service.computeAlpha(1L, 0.04).alpha();

        double pr = 0.75 * Math.log(2) / 5 * 252;
        double br = Math.log(1.5) / 5 * 252;
        assertThat(a0).isCloseTo(pr - 0.5 * br, within(1e-9));
        assertThat(a4).isCloseTo(pr - 0.5 * br - 0.02, within(1e-9));
    }

    private static List<Double> pricesFor(double... r) {
        List<Double> p = new java.util.ArrayList<>(List.of(100.0));
        for (double x : r) p.add(p.get(p.size() - 1) * Math.exp(x));
        return p;
    }

    @Test
    void shorterBenchmarkAlignsBothSeriesAtNewestEnd() {
        // portfolio 8 returns, benchmark 6 -> both cut to the newest 6.
        // p = {.40,-.20,.01,.02,.03,.04,.05,.06}; newest 6 sum .21 -> mean .035
        // (oldest-aligned first 6 would be .30/6 = .05; whole series .41/8 = .05125)
        // b mean = .02 ; beta 1, rf 0 -> alpha = (.035 - .02)*252
        Holding h = holding(1, "A");
        when(prices.requireHoldings(1L)).thenReturn(List.of(h));
        when(prices.weights(List.of(h))).thenReturn(new double[] {1.0});
        when(prices.assetPricesOldestFirst(1L))
                .thenReturn(pricesFor(0.40, -0.20, 0.01, 0.02, 0.03, 0.04, 0.05, 0.06));
        when(prices.benchmarkPricesOldestFirst()).thenReturn(pricesFor(0.02, 0.02, 0.02, 0.02, 0.02, 0.02));
        givenBeta(1.0);

        AlphaResponse r = service.computeAlpha(1L, 0.0);

        assertThat(r.portfolioReturn()).isCloseTo(0.035 * 252, within(1e-9));
        assertThat(r.benchmarkReturn()).isCloseTo(0.02 * 252, within(1e-9));
        assertThat(r.alpha()).isCloseTo(0.015 * 252, within(1e-9));
    }

    @Test
    void nonFiniteRiskFreeIsRejected() {
        assertThatThrownBy(() -> service.computeAlpha(1L, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.computeAlpha(1L, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.computeAlpha(1L, Double.NEGATIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void portfolioIdenticalToBenchmarkWithBetaOneHasZeroAlpha() {
        Holding h = holding(1, "A");
        when(prices.requireHoldings(1L)).thenReturn(List.of(h));
        when(prices.weights(List.of(h))).thenReturn(new double[] {1.0});
        when(prices.assetPricesOldestFirst(1L)).thenReturn(SPY);
        when(prices.benchmarkPricesOldestFirst()).thenReturn(SPY);
        givenBeta(1.0);

        // pr = br, so alpha = br - (rf + 1*(br - rf)) = 0 for any rf.
        assertThat(service.computeAlpha(1L, 0.03).alpha()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void fewerThanFiveSamplesGivesNullsButStillReportsBeta() {
        Holding h = holding(1, "A");
        when(prices.requireHoldings(1L)).thenReturn(List.of(h));
        when(prices.weights(List.of(h))).thenReturn(new double[] {1.0});
        when(prices.assetPricesOldestFirst(1L)).thenReturn(List.of(100.0, 90.0, 95.0, 80.0, 85.0)); // 4 returns
        givenBeta(1.3);

        AlphaResponse r = service.computeAlpha(1L, 0.02);

        assertThat(r.alpha()).isNull();
        assertThat(r.portfolioReturn()).isNull();
        assertThat(r.benchmarkReturn()).isNull();
        assertThat(r.beta()).isEqualTo(1.3);
    }

    @Test
    void shortBenchmarkHistoryGivesNulls() {
        Holding h = holding(1, "A");
        when(prices.requireHoldings(1L)).thenReturn(List.of(h));
        when(prices.weights(List.of(h))).thenReturn(new double[] {1.0});
        when(prices.assetPricesOldestFirst(1L)).thenReturn(A);
        when(prices.benchmarkPricesOldestFirst()).thenReturn(List.of(100.0, 101.0, 102.0));
        givenBeta(1.0);

        AlphaResponse r = service.computeAlpha(1L, 0.0);

        assertThat(r.alpha()).isNull();
        assertThat(r.portfolioReturn()).isNull();
        assertThat(r.benchmarkReturn()).isNull();
    }

    @Test
    void emptyPortfolioReturnsNullsAndBetaOne() {
        when(prices.requireHoldings(1L)).thenReturn(List.of());
        givenBeta(1.0);

        AlphaResponse r = service.computeAlpha(1L, 0.0);

        assertThat(r.alpha()).isNull();
        assertThat(r.portfolioReturn()).isNull();
        assertThat(r.benchmarkReturn()).isNull();
        assertThat(r.beta()).isEqualTo(1.0);
    }

    @Test
    void throwsWhenAccountNotFound() {
        when(prices.requireHoldings(99L)).thenThrow(new AccountNotFoundException("Account not found"));

        assertThatThrownBy(() -> service.computeAlpha(99L, 0.0)).isInstanceOf(AccountNotFoundException.class);
    }
}
