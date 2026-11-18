package com.finrisk.service;

import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.LookThroughResponse;
import com.finrisk.dto.response.LookThroughResponse.Exposure;
import com.finrisk.dto.response.OverlapResponse;
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

    private void givenEtfs(String... symbols) {
        List<Holding> hs = new java.util.ArrayList<>();
        for (int i = 0; i < symbols.length; i++) hs.add(h(i + 1, symbols[i], AssetType.ETF, 100));
        when(prices.requireHoldings(1L)).thenReturn(hs);
    }

    private static final List<EtfHolding> A = List.of(eh("AAPL", "0.5"), eh("MSFT", "0.3"), eh("NVDA", "0.2"));

    @Test
    void overlap_partial_usesMinOfUnequalWeights_andSortsShared() {
        // A: AAPL .5 MSFT .3 NVDA .2 ; B: AAPL .2 MSFT .4 GOOG .4
        // shared AAPL min(.5,.2)=.2, MSFT min(.3,.4)=.3 -> overlap .5; shared sorted MSFT(.3), AAPL(.2)
        givenEtfs("AAA", "BBB");
        when(provider.fetchEtfHoldings("AAA")).thenReturn(A);
        when(provider.fetchEtfHoldings("BBB")).thenReturn(List.of(eh("AAPL", "0.2"), eh("MSFT", "0.4"), eh("GOOG", "0.4")));

        OverlapResponse r = service.overlap(1L);

        assertThat(r.accountId()).isEqualTo(1L);
        assertThat(r.pairs()).hasSize(1);
        OverlapResponse.Pair p = r.pairs().get(0);
        assertThat(p.etfA()).isEqualTo("AAA");
        assertThat(p.etfB()).isEqualTo("BBB");
        assertThat(p.overlap()).isCloseTo(0.5, within(EPS));
        assertThat(p.sharedHoldings()).containsExactly("MSFT", "AAPL");
        assertThat(r.unresolvedEtfs()).isEmpty();
    }

    @Test
    void overlap_identicalEtfs_isOne() {
        // .5+.3+.2 = 1.0
        givenEtfs("AAA", "BBB");
        when(provider.fetchEtfHoldings("AAA")).thenReturn(A);
        when(provider.fetchEtfHoldings("BBB")).thenReturn(A);

        OverlapResponse.Pair p = service.overlap(1L).pairs().get(0);

        assertThat(p.overlap()).isCloseTo(1.0, within(EPS));
        assertThat(p.sharedHoldings()).containsExactly("AAPL", "MSFT", "NVDA");
    }

    @Test
    void overlap_disjointEtfs_isZeroWithNoShared() {
        givenEtfs("AAA", "BBB");
        when(provider.fetchEtfHoldings("AAA")).thenReturn(A);
        when(provider.fetchEtfHoldings("BBB")).thenReturn(List.of(eh("TSLA", "0.6"), eh("GOOG", "0.4")));

        OverlapResponse.Pair p = service.overlap(1L).pairs().get(0);

        assertThat(p.overlap()).isCloseTo(0.0, within(EPS));
        assertThat(p.sharedHoldings()).isEmpty();
    }

    @Test
    void overlap_threeEtfs_threePairsSortedDescending() {
        // A: AAPL .6 MSFT .4 ; B: AAPL .5 MSFT .3 NVDA .2 ; C: AAPL .3 NVDA .1 TSLA .6
        // AB = min(.6,.5)+min(.4,.3) = .5+.3 = .8 ; AC = min(.6,.3) = .3 ; BC = min(.5,.3)+min(.2,.1) = .3+.1 = .4
        givenEtfs("AAA", "BBB", "CCC");
        when(provider.fetchEtfHoldings("AAA")).thenReturn(List.of(eh("AAPL", "0.6"), eh("MSFT", "0.4")));
        when(provider.fetchEtfHoldings("BBB")).thenReturn(List.of(eh("AAPL", "0.5"), eh("MSFT", "0.3"), eh("NVDA", "0.2")));
        when(provider.fetchEtfHoldings("CCC")).thenReturn(List.of(eh("AAPL", "0.3"), eh("NVDA", "0.1"), eh("TSLA", "0.6")));

        List<OverlapResponse.Pair> ps = service.overlap(1L).pairs();

        assertThat(ps).extracting(p -> p.etfA() + p.etfB()).containsExactly("AAABBB", "BBBCCC", "AAACCC");
        assertThat(ps).extracting(OverlapResponse.Pair::overlap)
                .satisfiesExactly(o -> assertThat(o).isCloseTo(0.8, within(EPS)),
                        o -> assertThat(o).isCloseTo(0.4, within(EPS)),
                        o -> assertThat(o).isCloseTo(0.3, within(EPS)));
    }

    @Test
    void overlap_unresolvedEtf_excludedFromPairsAndReported() {
        givenEtfs("AAA", "BBB", "XYZ");
        when(provider.fetchEtfHoldings("AAA")).thenReturn(A);
        when(provider.fetchEtfHoldings("BBB")).thenReturn(A);
        when(provider.fetchEtfHoldings("XYZ")).thenReturn(List.of());

        OverlapResponse r = service.overlap(1L);

        assertThat(r.pairs()).hasSize(1);
        assertThat(r.unresolvedEtfs()).containsExactly("XYZ");
    }

    @Test
    void overlap_singleEtf_noPairs() {
        givenEtfs("AAA");
        when(provider.fetchEtfHoldings("AAA")).thenReturn(A);

        OverlapResponse r = service.overlap(1L);

        assertThat(r.pairs()).isEmpty();
        assertThat(r.unresolvedEtfs()).isEmpty();
    }

    @Test
    void overlap_providerCalledOncePerDistinctEtf_evenWithTwoLots() {
        // AAA held in two lots + BBB once -> 2 provider calls, 1 pair
        givenEtfs("AAA", "BBB", "AAA");
        when(provider.fetchEtfHoldings("AAA")).thenReturn(A);
        when(provider.fetchEtfHoldings("BBB")).thenReturn(A);

        OverlapResponse r = service.overlap(1L);

        assertThat(r.pairs()).hasSize(1);
        verify(provider, times(1)).fetchEtfHoldings("AAA");
        verify(provider, times(1)).fetchEtfHoldings("BBB");
    }

    @Test
    void overlap_accountNotFound_propagates() {
        when(prices.requireHoldings(9L)).thenThrow(new AccountNotFoundException("Account not found"));
        assertThatThrownBy(() -> service.overlap(9L)).isInstanceOf(AccountNotFoundException.class);
    }
}
