package com.finrisk.service;

import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.SectorAllocation;
import com.finrisk.dto.response.SectorExposureResponse;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.marketdata.CompanyProfile;
import com.finrisk.marketdata.MarketDataProvider;
import com.finrisk.model.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectorExposureServiceTest {

    @Mock PortfolioDao portfolioDao;
    @Mock MarketDataProvider marketDataProvider;

    SectorExposureService service;

    @BeforeEach
    void setUp() {
        service = new SectorExposureService(portfolioDao, marketDataProvider);
    }

    @Test
    void throwsWhenAccountNotFound() {
        when(portfolioDao.cashBalance(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.computeSectorExposure(99L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void returnsEmptyWhenNoHoldings() {
        when(portfolioDao.cashBalance(1L)).thenReturn(Optional.of(BigDecimal.valueOf(1000)));
        when(portfolioDao.holdings(1L)).thenReturn(List.of());

        SectorExposureResponse response = service.computeSectorExposure(1L);

        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.sectors()).isEmpty();
    }

    @Test
    void groupsHoldingsBySector() {
        Holding aapl = new Holding(1L, "AAPL", "Apple", AssetType.STOCK, 10,
                BigDecimal.valueOf(180), BigDecimal.valueOf(1800));
        Holding msft = new Holding(2L, "MSFT", "Microsoft", AssetType.STOCK, 5,
                BigDecimal.valueOf(300), BigDecimal.valueOf(1500));
        Holding jpm = new Holding(3L, "JPM", "JPMorgan", AssetType.STOCK, 8,
                BigDecimal.valueOf(200), BigDecimal.valueOf(1600));

        when(portfolioDao.cashBalance(1L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(portfolioDao.holdings(1L)).thenReturn(List.of(aapl, msft, jpm));
        when(marketDataProvider.fetchProfile("AAPL"))
                .thenReturn(new CompanyProfile("AAPL", "Technology", "Software", "Apple Inc."));
        when(marketDataProvider.fetchProfile("MSFT"))
                .thenReturn(new CompanyProfile("MSFT", "Technology", "Software", "Microsoft Corp."));
        when(marketDataProvider.fetchProfile("JPM"))
                .thenReturn(new CompanyProfile("JPM", "Financial Services", "Banks", "JPMorgan Chase"));

        SectorExposureResponse response = service.computeSectorExposure(1L);

        assertThat(response.sectors()).hasSize(2);

        // Sorted by weight desc: Technology (3300/4900 ≈ 67%) > Financial Services (1600/4900 ≈ 33%)
        SectorAllocation tech = response.sectors().get(0);
        assertThat(tech.sector()).isEqualTo("Technology");
        assertThat(tech.symbols()).containsExactlyInAnyOrder("AAPL", "MSFT");
        assertThat(tech.weight()).isGreaterThan(0.5);

        SectorAllocation fin = response.sectors().get(1);
        assertThat(fin.sector()).isEqualTo("Financial Services");
        assertThat(fin.symbols()).containsExactly("JPM");
    }

    @Test
    void weightsAddUpToApproximatelyOne() {
        Holding h1 = new Holding(1L, "AAPL", "Apple", AssetType.STOCK, 10,
                BigDecimal.valueOf(180), BigDecimal.valueOf(1800));
        Holding h2 = new Holding(2L, "JPM", "JPMorgan", AssetType.STOCK, 8,
                BigDecimal.valueOf(200), BigDecimal.valueOf(1600));

        when(portfolioDao.cashBalance(1L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(portfolioDao.holdings(1L)).thenReturn(List.of(h1, h2));
        when(marketDataProvider.fetchProfile("AAPL"))
                .thenReturn(new CompanyProfile("AAPL", "Technology", "Software", "Apple Inc."));
        when(marketDataProvider.fetchProfile("JPM"))
                .thenReturn(new CompanyProfile("JPM", "Financial Services", "Banks", "JPMorgan Chase"));

        SectorExposureResponse response = service.computeSectorExposure(1L);

        double total = response.sectors().stream().mapToDouble(SectorAllocation::weight).sum();
        assertThat(total).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
    }
}
