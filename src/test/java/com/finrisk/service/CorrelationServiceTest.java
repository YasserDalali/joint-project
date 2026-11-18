package com.finrisk.service;

import com.finrisk.dto.response.CorrelationResponse;
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
class CorrelationServiceTest {

    @Mock PortfolioPrices prices;

    CorrelationService service;

    // oldest-first, 6 log returns
    static final List<Double> BASE = List.of(100.0, 102.0, 101.0, 105.0, 104.0, 108.0, 107.0);
    static final List<Double> SQUARED = BASE.stream().map(p -> p * p).toList(); // returns = 2x base
    static final List<Double> INVERSE = BASE.stream().map(p -> 1 / p).toList(); // returns = -base
    static final List<Double> OTHER = List.of(50.0, 49.0, 51.0, 50.0, 52.0, 51.0, 53.0);
    static final List<Double> CONSTANT = List.of(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0);

    @BeforeEach
    void setUp() {
        service = new CorrelationService(prices);
    }

    private static Holding holding(long id, String symbol) {
        return new Holding(id, symbol, symbol, AssetType.STOCK, 1, BigDecimal.TEN, BigDecimal.TEN);
    }

    private void givenHoldings(Holding... hs) {
        when(prices.requireHoldings(1L)).thenReturn(List.of(hs));
    }

    private void givenSeries(long id, List<Double> series) {
        when(prices.assetPricesOldestFirst(id)).thenReturn(series);
    }

    @Test
    void perfectlyCorrelatedPairIsOne() {
        givenHoldings(holding(1, "A"), holding(2, "B"));
        givenSeries(1, BASE);
        givenSeries(2, SQUARED);

        CorrelationResponse r = service.computeCorrelation(1L);

        assertThat(r.symbols()).containsExactly("A", "B");
        assertThat(r.matrix().get(0).get(1)).isCloseTo(1.0, within(1e-9));
        assertThat(r.averagePairwiseCorrelation()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void perfectlyInversePairIsMinusOne() {
        givenHoldings(holding(1, "A"), holding(2, "B"));
        givenSeries(1, BASE);
        givenSeries(2, INVERSE);

        CorrelationResponse r = service.computeCorrelation(1L);

        assertThat(r.matrix().get(0).get(1)).isCloseTo(-1.0, within(1e-9));
        assertThat(r.averagePairwiseCorrelation()).isCloseTo(-1.0, within(1e-9));
    }

    @Test
    void threeHoldingsGiveSymmetricMatrixWithUnitDiagonal() {
        givenHoldings(holding(1, "A"), holding(2, "B"), holding(3, "C"));
        givenSeries(1, BASE);
        givenSeries(2, SQUARED);
        givenSeries(3, OTHER);

        List<List<Double>> m = service.computeCorrelation(1L).matrix();

        assertThat(m).hasSize(3);
        for (int i = 0; i < 3; i++) {
            assertThat(m.get(i)).hasSize(3);
            assertThat(m.get(i).get(i)).isEqualTo(1.0);
            for (int j = 0; j < 3; j++) {
                assertThat(m.get(i).get(j)).isCloseTo(m.get(j).get(i), within(1e-12));
            }
        }
        assertThat(m.get(0).get(1)).isCloseTo(1.0, within(1e-9));
        // A and C share the same corr as B and C because B returns = 2x A returns
        assertThat(m.get(0).get(2)).isCloseTo(m.get(1).get(2), within(1e-9));
    }

    @Test
    void constantPriceHoldingGivesNullEntriesAndIsExcludedFromAverage() {
        givenHoldings(holding(1, "A"), holding(2, "B"), holding(3, "K"));
        givenSeries(1, BASE);
        givenSeries(2, SQUARED);
        givenSeries(3, CONSTANT);

        CorrelationResponse r = service.computeCorrelation(1L);

        assertThat(r.matrix().get(0).get(2)).isNull();
        assertThat(r.matrix().get(2).get(0)).isNull();
        assertThat(r.matrix().get(2).get(1)).isNull();
        assertThat(r.averagePairwiseCorrelation()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void tooFewSamplesGivesNullEntry() {
        givenHoldings(holding(1, "A"), holding(2, "B"));
        givenSeries(1, BASE);
        givenSeries(2, List.of(10.0, 11.0, 12.0));

        CorrelationResponse r = service.computeCorrelation(1L);

        assertThat(r.matrix().get(0).get(1)).isNull();
        assertThat(r.averagePairwiseCorrelation()).isNull();
    }

    @Test
    void exactlyFourReturnSamplesIsUndefinedButFiveIsDefined() {
        // 5 prices -> 4 returns (below MIN_SAMPLES) ; 6 prices -> 5 returns (defined)
        givenHoldings(holding(1, "A"), holding(2, "B"));
        givenSeries(1, BASE.subList(0, 5));
        givenSeries(2, SQUARED.subList(0, 5));
        assertThat(service.computeCorrelation(1L).matrix().get(0).get(1)).isNull();

        givenSeries(1, BASE.subList(0, 6));
        givenSeries(2, SQUARED.subList(0, 6));
        assertThat(service.computeCorrelation(1L).matrix().get(0).get(1)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void singleHoldingHasNullAverage() {
        givenHoldings(holding(1, "A"));
        givenSeries(1, BASE);

        CorrelationResponse r = service.computeCorrelation(1L);

        assertThat(r.matrix()).containsExactly(List.of(1.0));
        assertThat(r.averagePairwiseCorrelation()).isNull();
    }

    @Test
    void emptyPortfolioReturnsEmpty() {
        givenHoldings();

        CorrelationResponse r = service.computeCorrelation(1L);

        assertThat(r.accountId()).isEqualTo(1L);
        assertThat(r.symbols()).isEmpty();
        assertThat(r.matrix()).isEmpty();
        assertThat(r.averagePairwiseCorrelation()).isNull();
    }

    @Test
    void throwsWhenAccountNotFound() {
        when(prices.requireHoldings(99L)).thenThrow(new AccountNotFoundException("Account not found"));

        assertThatThrownBy(() -> service.computeCorrelation(99L))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
