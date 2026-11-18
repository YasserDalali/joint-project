package com.finrisk.service;

import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.LookThroughResponse;
import com.finrisk.dto.response.LookThroughResponse.Exposure;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.marketdata.EtfHolding;
import com.finrisk.marketdata.MarketDataProvider;
import com.finrisk.model.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EtfLookThroughServiceTest {

    static final double EPS = 1e-9;

    @Mock PortfolioPrices prices;
    @Mock MarketDataProvider provider;

    EtfLookThroughService service;

    @BeforeEach
    void setUp() {
        service = new EtfLookThroughService(prices, provider);
    }

    private static Holding h(long id, String symbol, AssetType type, int value) {
        return new Holding(id, symbol, symbol, type, 1, BigDecimal.valueOf(value), BigDecimal.valueOf(value));
    }

    private static EtfHolding eh(String symbol, String weight) {
        return new EtfHolding(symbol, symbol + " Inc", new BigDecimal(weight));
    }

    private void given(List<Holding> hs, double... weights) {
        when(prices.requireHoldings(1L)).thenReturn(hs);
        when(prices.weights(hs)).thenReturn(weights);
    }

    private static Exposure find(LookThroughResponse r, String symbol) {
        return r.exposures().stream().filter(e -> e.symbol().equals(symbol)).findFirst().orElseThrow();
    }

    @Test
    void stockPlusEtfHoldingSameStock_sumsDirectAndVia() {
        // AAPL 600 (w=0.6), SPY 400 (w=0.4); SPY holds AAPL 10% and MSFT 5%.
        // AAPL: direct 0.6, via 0.4*0.10=0.04 -> effective 0.64; MSFT via 0.4*0.05=0.02; SPY residual not shown.
        List<Holding> hs = List.of(h(1, "AAPL", AssetType.STOCK, 600), h(2, "SPY", AssetType.ETF, 400));
        given(hs, 0.6, 0.4);
        when(provider.fetchEtfHoldings("SPY")).thenReturn(List.of(eh("AAPL", "0.10"), eh("MSFT", "0.05")));

        LookThroughResponse r = service.lookThrough(1L);

        assertThat(r.totalValue()).isEqualByComparingTo("1000");
        Exposure a = find(r, "AAPL");
        assertThat(a.directWeight()).isCloseTo(0.6, within(EPS));
        assertThat(a.viaEtfWeight()).isCloseTo(0.04, within(EPS));
        assertThat(a.effectiveWeight()).isCloseTo(0.64, within(EPS));
        assertThat(a.sources()).hasSize(1);
        assertThat(a.sources().get(0).etf()).isEqualTo("SPY");
        assertThat(a.sources().get(0).weight()).isCloseTo(0.04, within(EPS));
        Exposure m = find(r, "MSFT");
        assertThat(m.directWeight()).isCloseTo(0.0, within(EPS));
        assertThat(m.viaEtfWeight()).isCloseTo(0.02, within(EPS));
        assertThat(r.unresolvedEtfs()).isEmpty();
        assertThat(r.exposures().get(0).symbol()).isEqualTo("AAPL");
    }

    @Test
    void twoEtfsSharingStock_sumsViaWithBothSources() {
        // SPY w=0.5 holds AAPL 8% -> 0.04 ; QQQ w=0.3 holds AAPL 10% -> 0.03 ; AAPL direct w=0.2.
        // AAPL via = 0.07, effective = 0.27
        List<Holding> hs = List.of(h(1, "SPY", AssetType.ETF, 500), h(2, "QQQ", AssetType.ETF, 300),
                h(3, "AAPL", AssetType.STOCK, 200));
        given(hs, 0.5, 0.3, 0.2);
        when(provider.fetchEtfHoldings("SPY")).thenReturn(List.of(eh("AAPL", "0.08")));
        when(provider.fetchEtfHoldings("QQQ")).thenReturn(List.of(eh("AAPL", "0.10")));

        Exposure a = find(service.lookThrough(1L), "AAPL");

        assertThat(a.directWeight()).isCloseTo(0.2, within(EPS));
        assertThat(a.viaEtfWeight()).isCloseTo(0.07, within(EPS));
        assertThat(a.effectiveWeight()).isCloseTo(0.27, within(EPS));
        assertThat(a.sources()).extracting(LookThroughResponse.Source::etf).containsExactlyInAnyOrder("SPY", "QQQ");
        assertThat(a.sources()).extracting(LookThroughResponse.Source::weight)
                .containsExactlyInAnyOrder(0.04, 0.03);
    }

    @Test
    void unresolvedEtf_fallsBackToDirectAndIsReported() {
        // XYZ ETF w=0.25 unresolved -> direct exposure 0.25 under XYZ; AAPL w=0.75.
        List<Holding> hs = List.of(h(1, "AAPL", AssetType.STOCK, 750), h(2, "XYZ", AssetType.ETF, 250));
        given(hs, 0.75, 0.25);
        when(provider.fetchEtfHoldings("XYZ")).thenReturn(List.of());

        LookThroughResponse r = service.lookThrough(1L);

        assertThat(r.unresolvedEtfs()).containsExactly("XYZ");
        Exposure x = find(r, "XYZ");
        assertThat(x.directWeight()).isCloseTo(0.25, within(EPS));
        assertThat(x.viaEtfWeight()).isCloseTo(0.0, within(EPS));
        assertThat(x.effectiveWeight()).isCloseTo(0.25, within(EPS));
        assertThat(r.exposures().stream().mapToDouble(Exposure::effectiveWeight).sum()).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void effectiveWeightsSumToOne_andSortedDescending() {
        // SPY w=0.4 resolved fully: AAPL 0.5, MSFT 0.3, NVDA 0.2 -> 0.2, 0.12, 0.08. Direct GOOG w=0.15, AMZN w=0.45.
        // Sorted: AMZN .45, AAPL .2, GOOG .15, MSFT .12, NVDA .08 (sum 1.0)
        List<Holding> hs = List.of(h(1, "SPY", AssetType.ETF, 400), h(2, "GOOG", AssetType.STOCK, 150),
                h(3, "AMZN", AssetType.STOCK, 450));
        given(hs, 0.4, 0.15, 0.45);
        when(provider.fetchEtfHoldings("SPY"))
                .thenReturn(List.of(eh("AAPL", "0.5"), eh("MSFT", "0.3"), eh("NVDA", "0.2")));

        LookThroughResponse r = service.lookThrough(1L);

        assertThat(r.exposures()).extracting(Exposure::symbol)
                .containsExactly("AMZN", "AAPL", "GOOG", "MSFT", "NVDA");
        assertThat(r.exposures().stream().mapToDouble(Exposure::effectiveWeight).sum()).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void fetchEtfHoldings_fetchesEachEtfOnceKeyedBySymbol() {
        List<Holding> hs = List.of(h(1, "SPY", AssetType.ETF, 1), h(2, "AAPL", AssetType.STOCK, 1),
                h(3, "SPY", AssetType.ETF, 1));
        when(provider.fetchEtfHoldings("SPY")).thenReturn(List.of(eh("AAPL", "0.1")));

        Map<String, List<EtfHolding>> m = service.fetchEtfHoldings(hs);

        assertThat(m).containsOnlyKeys("SPY");
        assertThat(m.get("SPY")).hasSize(1);
        verify(provider, times(1)).fetchEtfHoldings("SPY");
    }

    @Test
    void accountNotFound_propagates() {
        when(prices.requireHoldings(9L)).thenThrow(new AccountNotFoundException("Account not found"));
        assertThatThrownBy(() -> service.lookThrough(9L)).isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void emptyPortfolio_returnsEmpty() {
        given(List.of());
        LookThroughResponse r = service.lookThrough(1L);
        assertThat(r.totalValue()).isEqualByComparingTo("0");
        assertThat(r.exposures()).isEmpty();
        assertThat(r.unresolvedEtfs()).isEmpty();
    }
}
