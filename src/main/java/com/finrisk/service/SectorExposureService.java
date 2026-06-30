package com.finrisk.service;

import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.SectorAllocation;
import com.finrisk.dto.response.SectorExposureResponse;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.marketdata.CompanyProfile;
import com.finrisk.marketdata.MarketDataProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class SectorExposureService {

    private final PortfolioDao portfolioDao;
    private final MarketDataProvider marketDataProvider;

    public SectorExposureService(PortfolioDao portfolioDao, MarketDataProvider marketDataProvider) {
        this.portfolioDao = portfolioDao;
        this.marketDataProvider = marketDataProvider;
    }

    public SectorExposureResponse computeSectorExposure(long accountId) {
        if (portfolioDao.cashBalance(accountId).isEmpty()) {
            throw new AccountNotFoundException("Account not found");
        }

        List<Holding> holdings = portfolioDao.holdings(accountId);
        if (holdings.isEmpty()) {
            return new SectorExposureResponse(accountId, List.of());
        }

        BigDecimal totalValue = holdings.stream()
                .map(Holding::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> sectorValue = new LinkedHashMap<>();
        Map<String, List<String>> sectorSymbols = new LinkedHashMap<>();

        for (Holding h : holdings) {
            CompanyProfile profile = marketDataProvider.fetchProfile(h.symbol());
            String sector = profile.sector();
            sectorValue.merge(sector, h.currentValue(), BigDecimal::add);
            sectorSymbols.computeIfAbsent(sector, k -> new ArrayList<>()).add(h.symbol());
        }

        List<SectorAllocation> allocations = sectorValue.entrySet().stream()
                .map(e -> {
                    double weight = totalValue.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                            : e.getValue().divide(totalValue, 8, RoundingMode.HALF_UP).doubleValue();
                    return new SectorAllocation(e.getKey(), weight, e.getValue(), sectorSymbols.get(e.getKey()));
                })
                .sorted(Comparator.comparingDouble(SectorAllocation::weight).reversed())
                .toList();

        return new SectorExposureResponse(accountId, allocations);
    }
}
