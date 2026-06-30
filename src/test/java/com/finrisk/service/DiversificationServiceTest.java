package com.finrisk.service;

import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.AssetTypeAllocation;
import com.finrisk.dto.response.DiversificationResponse;
import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.SectorAllocation;
import com.finrisk.dto.response.SectorExposureResponse;
import com.finrisk.exception.AccountNotFoundException;
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
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiversificationServiceTest {

    @Mock PortfolioDao portfolioDao;
    @Mock SectorExposureService sectorExposureService;

    DiversificationService service;

    @BeforeEach
    void setUp() {
        service = new DiversificationService(portfolioDao, sectorExposureService);
    }

    @Test
    void throwsWhenAccountNotFound() {
        when(portfolioDao.cashBalance(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.computeDiversification(99L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void scoreIsZeroWhenSingleHolding() {
        // Single holding: weight = 1.0, HHI = 1.0, score = 0
        Holding h = new Holding(1L, "AAPL", "Apple", AssetType.STOCK, 10,
                BigDecimal.valueOf(180), BigDecimal.valueOf(1800));
        when(portfolioDao.cashBalance(1L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(portfolioDao.holdings(1L)).thenReturn(List.of(h));
        when(sectorExposureService.computeSectorExposure(1L))
                .thenReturn(new SectorExposureResponse(1L, List.of(
                        new SectorAllocation("Technology", 1.0, BigDecimal.valueOf(1800), List.of("AAPL")))));

        DiversificationResponse response = service.computeDiversification(1L);

        assertThat(response.score()).isCloseTo(0.0, offset(0.01));
        assertThat(response.hhiIndex()).isCloseTo(1.0, offset(0.01));
        assertThat(response.holdingCount()).isEqualTo(1);
        assertThat(response.sectorCount()).isEqualTo(1);
        assertThat(response.assetTypeCount()).isEqualTo(1);
    }

    @Test
    void scoreIsHigherWithEquallyWeightedHoldings() {
        // 4 equal holdings: each weight = 0.25, HHI = 4 × 0.0625 = 0.25, score = 75
        BigDecimal val = BigDecimal.valueOf(1000);
        List<Holding> holdings = List.of(
                new Holding(1L, "A", "A", AssetType.STOCK, 1, val, val),
                new Holding(2L, "B", "B", AssetType.STOCK, 1, val, val),
                new Holding(3L, "C", "C", AssetType.ETF, 1, val, val),
                new Holding(4L, "D", "D", AssetType.BOND, 1, val, val)
        );
        when(portfolioDao.cashBalance(1L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(portfolioDao.holdings(1L)).thenReturn(holdings);
        when(sectorExposureService.computeSectorExposure(1L))
                .thenReturn(new SectorExposureResponse(1L, List.of(
                        new SectorAllocation("Technology", 0.5, BigDecimal.valueOf(2000), List.of("A", "B")),
                        new SectorAllocation("Bonds", 0.25, BigDecimal.valueOf(1000), List.of("C")),
                        new SectorAllocation("Other", 0.25, BigDecimal.valueOf(1000), List.of("D")))));

        DiversificationResponse response = service.computeDiversification(1L);

        assertThat(response.score()).isCloseTo(75.0, offset(0.01));
        assertThat(response.hhiIndex()).isCloseTo(0.25, offset(0.01));
        assertThat(response.holdingCount()).isEqualTo(4);
        assertThat(response.sectorCount()).isEqualTo(3);
        assertThat(response.assetTypeCount()).isEqualTo(3); // STOCK, ETF, BOND
    }

    @Test
    void assetTypeBreakdownSortedByWeightDesc() {
        BigDecimal bigVal = BigDecimal.valueOf(3000);
        BigDecimal smallVal = BigDecimal.valueOf(1000);
        List<Holding> holdings = List.of(
                new Holding(1L, "A", "A", AssetType.STOCK, 1, bigVal, bigVal),
                new Holding(2L, "B", "B", AssetType.BOND, 1, smallVal, smallVal)
        );
        when(portfolioDao.cashBalance(1L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(portfolioDao.holdings(1L)).thenReturn(holdings);
        when(sectorExposureService.computeSectorExposure(1L))
                .thenReturn(new SectorExposureResponse(1L, List.of()));

        DiversificationResponse response = service.computeDiversification(1L);

        List<AssetTypeAllocation> breakdown = response.assetTypeBreakdown();
        assertThat(breakdown.get(0).assetType()).isEqualTo("STOCK");
        assertThat(breakdown.get(0).weight()).isCloseTo(0.75, offset(0.01));
        assertThat(breakdown.get(1).assetType()).isEqualTo("BOND");
        assertThat(breakdown.get(1).weight()).isCloseTo(0.25, offset(0.01));
    }
}
