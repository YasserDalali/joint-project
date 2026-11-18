package com.finrisk.service;

import com.finrisk.dto.response.DrawdownResponse;
import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.HoldingDrawdown;
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
class DrawdownServiceTest {

    @Mock PortfolioPrices prices;

    DrawdownService service;

    @BeforeEach
    void setUp() {
        service = new DrawdownService(prices);
    }

    private static Holding holding(long id, String symbol) {
        return new Holding(id, symbol, symbol, AssetType.STOCK, 1, BigDecimal.TEN, BigDecimal.TEN);
    }

    /** One holding with weight 1.0 so the portfolio series equals the holding's series. */
    private void givenSingleHolding(List<Double> series) {
        Holding h = holding(1, "A");
        when(prices.requireHoldings(1L)).thenReturn(List.of(h));
        when(prices.weights(List.of(h))).thenReturn(new double[] {1.0});
        when(prices.assetPricesOldestFirst(1L)).thenReturn(series);
    }

    @Test
    void riseThenTwentyPercentFallGivesMinusTwentyAndCurrentEqualsMax() {
        givenSingleHolding(List.of(100.0, 105.0, 110.0, 120.0, 110.0, 96.0)); // 96/120 - 1 = -0.2

        DrawdownResponse r = service.computeDrawdown(1L);

        assertThat(r.accountId()).isEqualTo(1L);
        assertThat(r.maxDrawdown()).isCloseTo(-0.20, within(1e-9));
        assertThat(r.currentDrawdown()).isCloseTo(-0.20, within(1e-9));
    }

    @Test
    void perHoldingMaxDrawdownIsNullWhenFewerThanTwoPrices() {
        Holding a = holding(1, "A"), b = holding(2, "B");
        when(prices.requireHoldings(1L)).thenReturn(List.of(a, b));
        when(prices.weights(List.of(a, b))).thenReturn(new double[] {0.5, 0.5});
        when(prices.assetPricesOldestFirst(1L)).thenReturn(List.of(100.0, 120.0, 90.0, 95.0)); // 90/120 - 1 = -0.25
        when(prices.assetPricesOldestFirst(2L)).thenReturn(List.of(50.0));

        List<HoldingDrawdown> h = service.computeDrawdown(1L).holdings();

        assertThat(h).hasSize(2);
        assertThat(h.get(0).assetId()).isEqualTo(1L);
        assertThat(h.get(0).symbol()).isEqualTo("A");
        assertThat(h.get(0).maxDrawdown()).isCloseTo(-0.25, within(1e-9));
        assertThat(h.get(1).symbol()).isEqualTo("B");
        assertThat(h.get(1).maxDrawdown()).isNull();
    }

    @Test
    void monotonicRisingSeriesGivesZero() {
        givenSingleHolding(List.of(100.0, 101.0, 102.0, 103.0, 104.0, 105.0));

        DrawdownResponse r = service.computeDrawdown(1L);

        assertThat(r.maxDrawdown()).isCloseTo(0.0, within(1e-12));
        assertThat(r.currentDrawdown()).isCloseTo(0.0, within(1e-12));
    }

    @Test
    void recoveryAfterTroughResetsCurrentButKeepsMax() {
        givenSingleHolding(List.of(100.0, 120.0, 96.0, 100.0, 125.0, 126.0)); // trough 96/120-1 = -0.2, new high 125

        DrawdownResponse r = service.computeDrawdown(1L);

        assertThat(r.maxDrawdown()).isCloseTo(-0.20, within(1e-9));
        assertThat(r.currentDrawdown()).isCloseTo(126.0 / 126.0 - 1, within(1e-12));
    }

    @Test
    void fewerThanFiveReturnSamplesGivesNullPortfolioFigures() {
        givenSingleHolding(List.of(100.0, 90.0, 95.0, 80.0, 85.0)); // 4 returns

        DrawdownResponse r = service.computeDrawdown(1L);

        assertThat(r.maxDrawdown()).isNull();
        assertThat(r.currentDrawdown()).isNull();
        assertThat(r.holdings().get(0).maxDrawdown()).isCloseTo(-0.2, within(1e-9));
    }

    @Test
    void emptyPortfolioReturnsNullsAndNoHoldings() {
        when(prices.requireHoldings(1L)).thenReturn(List.of());

        DrawdownResponse r = service.computeDrawdown(1L);

        assertThat(r.maxDrawdown()).isNull();
        assertThat(r.currentDrawdown()).isNull();
        assertThat(r.holdings()).isEmpty();
    }

    @Test
    void throwsWhenAccountNotFound() {
        when(prices.requireHoldings(99L)).thenThrow(new AccountNotFoundException("Account not found"));

        assertThatThrownBy(() -> service.computeDrawdown(99L)).isInstanceOf(AccountNotFoundException.class);
    }
}
