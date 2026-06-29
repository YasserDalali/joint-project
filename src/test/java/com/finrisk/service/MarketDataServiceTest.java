package com.finrisk.service;

import com.finrisk.dao.AssetDao;
import com.finrisk.dao.AssetPriceHistoryDao;
import com.finrisk.exception.AssetNotFoundException;
import com.finrisk.marketdata.MarketDataProvider;
import com.finrisk.marketdata.PricePoint;
import com.finrisk.model.Asset;
import com.finrisk.model.RiskLevel;
import com.finrisk.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock MarketDataProvider provider;
    @Mock AssetDao assetDao;
    @Mock AssetPriceHistoryDao priceHistoryDao;

    MarketDataService service;

    @BeforeEach
    void setUp() {
        service = new MarketDataService(provider, assetDao, priceHistoryDao);
    }

    @Test
    void syncPrices_throwsWhenAssetNotFound() {
        when(assetDao.findById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.syncPrices(99L, 30))
                .isInstanceOf(AssetNotFoundException.class);
    }

    @Test
    void syncPrices_upsertsAllReturnedPoints() {
        // Asset is a sealed interface; use a concrete Stock record instead of a mock
        Asset asset = new Stock(1L, "AAPL", "Apple Inc.", BigDecimal.TEN,
                RiskLevel.HIGH, LocalDateTime.now(), "Tech", "NASDAQ");
        when(assetDao.findById(1L)).thenReturn(asset);
        List<PricePoint> points = List.of(
                new PricePoint(LocalDate.of(2024, 6, 27), new BigDecimal("180.00")),
                new PricePoint(LocalDate.of(2024, 6, 28), new BigDecimal("182.50"))
        );
        when(provider.fetchDailyPrices("AAPL", 30)).thenReturn(points);

        int synced = service.syncPrices(1L, 30);

        assertThat(synced).isEqualTo(2);
        verify(priceHistoryDao).upsertForDate(1L, LocalDate.of(2024, 6, 27), new BigDecimal("180.00"));
        verify(priceHistoryDao).upsertForDate(1L, LocalDate.of(2024, 6, 28), new BigDecimal("182.50"));
    }

    @Test
    void syncPrices_returnsZeroWhenProviderReturnsEmpty() {
        // Asset is a sealed interface; use a concrete Stock record instead of a mock
        Asset asset = new Stock(1L, "UNKNOWN", "Unknown Co.", BigDecimal.TEN,
                RiskLevel.LOW, LocalDateTime.now(), "Other", "NYSE");
        when(assetDao.findById(1L)).thenReturn(asset);
        when(provider.fetchDailyPrices("UNKNOWN", 30)).thenReturn(List.of());

        int synced = service.syncPrices(1L, 30);

        assertThat(synced).isEqualTo(0);
        verifyNoInteractions(priceHistoryDao);
    }
}
