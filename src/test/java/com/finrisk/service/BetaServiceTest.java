package com.finrisk.service;

import com.finrisk.dao.AssetPriceHistoryDao;
import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.BetaResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetaServiceTest {

    @Mock PortfolioDao portfolioDao;
    @Mock AssetPriceHistoryDao priceHistoryDao;
    @Mock MarketDataProvider marketDataProvider;

    BetaService service;

    @BeforeEach
    void setUp() {
        service = new BetaService(portfolioDao, priceHistoryDao, marketDataProvider);
    }

    @Test
    void throwsWhenAccountNotFound() {
        when(portfolioDao.cashBalance(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.computeBeta(99L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void returnsPortfolioBetaOneWhenNoHoldings() {
        when(portfolioDao.cashBalance(1L)).thenReturn(Optional.of(BigDecimal.valueOf(1000)));
        when(portfolioDao.holdings(1L)).thenReturn(List.of());

        BetaResponse response = service.computeBeta(1L);

        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.portfolioBeta()).isEqualTo(1.0);
        assertThat(response.benchmark()).isEqualTo("SPY");
        assertThat(response.holdings()).isEmpty();
    }

    @Test
    void usesFallbackBetaWhenInsufficientPriceHistory() {
        Holding h = new Holding(1L, "AAPL", "Apple", AssetType.STOCK, 10,
                BigDecimal.valueOf(180), BigDecimal.valueOf(1800));
        when(portfolioDao.cashBalance(1L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(portfolioDao.holdings(1L)).thenReturn(List.of(h));
        when(priceHistoryDao.latestPrices(1L, 90)).thenReturn(List.of(BigDecimal.valueOf(180)));
        when(marketDataProvider.fetchDailyPrices("SPY", 90)).thenReturn(List.of());

        BetaResponse response = service.computeBeta(1L);

        assertThat(response.holdings()).hasSize(1);
        assertThat(response.holdings().get(0).beta()).isEqualTo(1.0);
    }

    @Test
    void computesNonTrivialBetaFromPriceSeries() {
        // Build prices where asset log returns = 2× SPY log returns → beta = 2.0
        double[] spyLogReturns = {0.02, -0.01, 0.03, -0.02, 0.01, -0.03, 0.02, 0.01, -0.015};
        List<BigDecimal> spyChron = pricesFromLogReturns(100.0, spyLogReturns);
        double[] assetLogReturns = new double[spyLogReturns.length];
        for (int i = 0; i < spyLogReturns.length; i++) assetLogReturns[i] = 2 * spyLogReturns[i];
        List<BigDecimal> assetChron = pricesFromLogReturns(200.0, assetLogReturns);

        // latestPrices and fetchDailyPrices return newest-first
        List<BigDecimal> assetNewest = new ArrayList<>(assetChron).reversed();
        List<BigDecimal> spyNewest = new ArrayList<>(spyChron).reversed();

        List<PricePoint> spyPoints = new ArrayList<>();
        for (int i = 0; i < spyNewest.size(); i++) {
            spyPoints.add(new PricePoint(LocalDate.now().minusDays(i), spyNewest.get(i)));
        }

        Holding h = new Holding(1L, "AAPL", "Apple", AssetType.STOCK, 10,
                assetNewest.get(0), assetNewest.get(0).multiply(BigDecimal.TEN));
        when(portfolioDao.cashBalance(1L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(portfolioDao.holdings(1L)).thenReturn(List.of(h));
        when(priceHistoryDao.latestPrices(1L, 90)).thenReturn(assetNewest);
        when(marketDataProvider.fetchDailyPrices("SPY", 90)).thenReturn(spyPoints);

        BetaResponse response = service.computeBeta(1L);

        assertThat(response.holdings().get(0).beta()).isCloseTo(2.0, org.assertj.core.data.Offset.offset(0.05));
        assertThat(response.portfolioBeta()).isCloseTo(2.0, org.assertj.core.data.Offset.offset(0.05));
    }

    private static List<BigDecimal> pricesFromLogReturns(double start, double[] returns) {
        List<BigDecimal> prices = new ArrayList<>();
        prices.add(BigDecimal.valueOf(Math.round(start * 100.0) / 100.0));
        double p = start;
        for (double r : returns) {
            p = p * Math.exp(r);
            prices.add(BigDecimal.valueOf(Math.round(p * 100.0) / 100.0));
        }
        return prices;
    }

    private static BigDecimal bd(String v) { return new BigDecimal(v); }
}
