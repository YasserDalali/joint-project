package com.finrisk.service;

import com.finrisk.dao.AssetPriceHistoryDao;
import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.Holding;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.marketdata.MarketDataProvider;
import com.finrisk.marketdata.PricePoint;
import com.finrisk.model.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioPricesTest {

    @Mock PortfolioDao portfolioDao;
    @Mock AssetPriceHistoryDao priceHistoryDao;
    @Mock MarketDataProvider marketDataProvider;

    PortfolioPrices prices;

    @BeforeEach
    void setUp() {
        prices = new PortfolioPrices(portfolioDao, priceHistoryDao, marketDataProvider);
    }

    private static Holding holding(long id, String value) {
        return new Holding(id, "S" + id, "N" + id, AssetType.STOCK, 1,
                new BigDecimal(value), new BigDecimal(value));
    }

    @Test
    void requireHoldingsThrowsWhenAccountMissing() {
        when(portfolioDao.cashBalance(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prices.requireHoldings(99L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found");
    }

    @Test
    void requireHoldingsReturnsDaoHoldings() {
        List<Holding> hs = List.of(holding(1, "100"));
        when(portfolioDao.cashBalance(1L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(portfolioDao.holdings(1L)).thenReturn(hs);

        assertThat(prices.requireHoldings(1L)).isEqualTo(hs);
    }

    @Test
    void weightsAreValueShares() {
        double[] w = prices.weights(List.of(holding(1, "300"), holding(2, "100")));

        assertThat(w[0]).isCloseTo(0.75, within(1e-9));
        assertThat(w[1]).isCloseTo(0.25, within(1e-9));
    }

    @Test
    void weightsAreZeroWhenTotalIsZero() {
        assertThat(prices.weights(List.of(holding(1, "0"), holding(2, "0")))).containsExactly(0.0, 0.0);
    }

    @Test
    void assetPricesAreReversedToOldestFirst() {
        when(priceHistoryDao.latestPrices(7L, 90))
                .thenReturn(List.of(new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("10")));

        assertThat(prices.assetPricesOldestFirst(7L)).containsExactly(10.0, 20.0, 30.0);
    }

    @Test
    void benchmarkPricesAreSpyReversedToOldestFirst() {
        when(marketDataProvider.fetchDailyPrices("SPY", 90)).thenReturn(List.of(
                new PricePoint(LocalDate.of(2026, 1, 3), new BigDecimal("3")),
                new PricePoint(LocalDate.of(2026, 1, 2), new BigDecimal("2")),
                new PricePoint(LocalDate.of(2026, 1, 1), new BigDecimal("1"))));

        assertThat(prices.benchmarkPricesOldestFirst()).containsExactly(1.0, 2.0, 3.0);
    }

    @Test
    void exposesConstants() {
        assertThat(PortfolioPrices.BENCHMARK).isEqualTo("SPY");
        assertThat(PortfolioPrices.PRICE_DAYS).isEqualTo(90);
        assertThat(PortfolioPrices.MIN_SAMPLES).isEqualTo(5);
    }
}
