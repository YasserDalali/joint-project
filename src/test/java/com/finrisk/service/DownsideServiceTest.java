package com.finrisk.service;

import com.finrisk.dto.response.DownsideResponse;
import com.finrisk.dto.response.Holding;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.model.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownsideServiceTest {

    @Mock PortfolioPrices prices;

    DownsideService service;

    @BeforeEach
    void setUp() {
        service = new DownsideService(prices);
    }

    private static Holding holding(long id, String symbol) {
        return new Holding(id, symbol, symbol, AssetType.STOCK, 1, BigDecimal.TEN, BigDecimal.TEN);
    }

    /** Price series (starting at 100) whose daily log returns are exactly r. */
    private static List<Double> pricesFor(double... r) {
        List<Double> p = new ArrayList<>();
        double v = 100.0;
        p.add(v);
        for (double x : r) {
            v *= Math.exp(x);
            p.add(v);
        }
        return p;
    }

    private void givenSingleHolding(double[] rp, double[] rm) {
        Holding h = holding(1, "A");
        when(prices.requireHoldings(1L)).thenReturn(List.of(h));
        when(prices.weights(List.of(h))).thenReturn(new double[] {1.0});
        when(prices.assetPricesOldestFirst(1L)).thenReturn(pricesFor(rp));
        when(prices.benchmarkPricesOldestFirst()).thenReturn(pricesFor(rm));
    }

    private static final double[] RM_FLAT = {0.01, 0.01, 0.01, 0.01, 0.01};

    @Test
    void downsideDeviationSortinoAndWorstDayForSmallSeries() {
        // rp = [0.01, -0.02, 0.03, -0.04, 0.05]; target 0.
        // sum of min(rp,0)^2 = 0.0004 + 0.0016 = 0.002 ; /n(5) = 0.0004 ; sqrt = 0.02
        // downsideDeviation = 0.02*sqrt(252)
        // mean(rp) = 0.03/5 = 0.006 ; sortino = 0.006*252/(0.02*sqrt(252)) = 0.3*sqrt(252)
        // worstDay = -0.04
        givenSingleHolding(new double[] {0.01, -0.02, 0.03, -0.04, 0.05}, RM_FLAT);

        DownsideResponse r = service.computeDownside(1L);

        assertThat(r.accountId()).isEqualTo(1L);
        assertThat(r.downsideDeviation()).isCloseTo(0.02 * Math.sqrt(252), within(1e-9));
        assertThat(r.sortinoRatio()).isCloseTo(0.3 * Math.sqrt(252), within(1e-9));
        assertThat(r.worstDay()).isCloseTo(-0.04, within(1e-12));
        assertThat(r.benchmark()).isEqualTo("SPY");
    }

    @Test
    void allPositiveReturnsGiveZeroDownsideDeviationAndNullSortino() {
        // no negative day -> sum = 0 -> downsideDeviation 0.0 ; sortino undefined (divide by 0) -> null
        givenSingleHolding(new double[] {0.01, 0.02, 0.03, 0.04, 0.05}, RM_FLAT);

        DownsideResponse r = service.computeDownside(1L);

        assertThat(r.downsideDeviation()).isEqualTo(0.0);
        assertThat(r.sortinoRatio()).isNull();
    }

    @Test
    void downsideBetaIsTwoWhenPortfolioIsTwiceSpyOnDownDays() {
        // rm has 5 down days: -0.01 -0.02 -0.03 -0.015 -0.025 (plus up days 0.01, 0.02).
        // Two holdings, weights 0.75/0.25. Per day x_t varies:
        //   A = 2rm + x (+0.05 on up days), B = 2rm - 3x (+0.05 on up days)
        //   rp = 0.75A + 0.25B = 2rm + 0.75x - 0.75x (+0.05 up) = 2rm on down days.
        // (with a wrong equal weighting rp would be 2rm - x, so the slope would not be 2.)
        // On down days cov(rp,rm)/var(rm) = 2 exactly. Up days are polluted (+0.05) so an
        // unfiltered regression would not give 2.
        double[] rm = {-0.01, 0.01, -0.02, -0.03, 0.02, -0.015, -0.025};
        double[] x = {0.01, -0.02, 0.03, -0.01, 0.02, 0.04, -0.03};
        double[] a = new double[rm.length], b = new double[rm.length];
        for (int i = 0; i < rm.length; i++) {
            double up = rm[i] >= 0 ? 0.05 : 0.0;
            a[i] = 2 * rm[i] + x[i] + up;
            b[i] = 2 * rm[i] - 3 * x[i] + up;
        }
        Holding h1 = holding(1, "A"), h2 = holding(2, "B");
        when(prices.requireHoldings(1L)).thenReturn(List.of(h1, h2));
        when(prices.weights(List.of(h1, h2))).thenReturn(new double[] {0.75, 0.25});
        when(prices.assetPricesOldestFirst(1L)).thenReturn(pricesFor(a));
        when(prices.assetPricesOldestFirst(2L)).thenReturn(pricesFor(b));
        when(prices.benchmarkPricesOldestFirst()).thenReturn(pricesFor(rm));

        DownsideResponse r = service.computeDownside(1L);

        assertThat(r.downsideBeta()).isCloseTo(2.0, within(1e-9));
    }

    @Test
    void fewerThanFiveDownDaysGivesNullDownsideBetaButOtherMetrics() {
        // rm has only 4 negative days among 6.
        double[] rm = {-0.01, -0.02, 0.01, -0.03, -0.02, 0.02};
        double[] rp = {-0.02, -0.04, 0.01, -0.06, -0.04, 0.03};
        givenSingleHolding(rp, rm);

        DownsideResponse r = service.computeDownside(1L);

        assertThat(r.downsideBeta()).isNull();
        assertThat(r.downsideDeviation()).isNotNull();
        assertThat(r.worstDay()).isCloseTo(-0.06, within(1e-12));
    }

    @Test
    void fewerThanFiveSamplesGivesAllNulls() {
        Holding h = holding(1, "A");
        when(prices.requireHoldings(1L)).thenReturn(List.of(h));
        when(prices.weights(List.of(h))).thenReturn(new double[] {1.0});
        when(prices.assetPricesOldestFirst(1L)).thenReturn(pricesFor(0.01, -0.02, 0.03, -0.04)); // 4 returns

        DownsideResponse r = service.computeDownside(1L);

        assertThat(r.downsideDeviation()).isNull();
        assertThat(r.sortinoRatio()).isNull();
        assertThat(r.downsideBeta()).isNull();
        assertThat(r.worstDay()).isNull();
        assertThat(r.benchmark()).isEqualTo("SPY");
    }

    @Test
    void emptyPortfolioGivesAllNulls() {
        when(prices.requireHoldings(1L)).thenReturn(List.of());

        DownsideResponse r = service.computeDownside(1L);

        assertThat(r.downsideDeviation()).isNull();
        assertThat(r.sortinoRatio()).isNull();
        assertThat(r.downsideBeta()).isNull();
        assertThat(r.worstDay()).isNull();
    }

    @Test
    void throwsWhenAccountNotFound() {
        when(prices.requireHoldings(99L)).thenThrow(new AccountNotFoundException("Account not found"));

        assertThatThrownBy(() -> service.computeDownside(99L)).isInstanceOf(AccountNotFoundException.class);
    }
}
