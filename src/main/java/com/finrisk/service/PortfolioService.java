package com.finrisk.service;

import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.PortfolioResponse;
import com.finrisk.exception.AccountNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Aggregates cash plus holdings into dashboard-ready {@link PortfolioResponse} snapshots. */
@Service
public class PortfolioService {

    private final PortfolioDao portfolioDao;

    /** Provides read-only portfolio projections sourced from SQL joins. */
    public PortfolioService(PortfolioDao portfolioDao) {
        this.portfolioDao = portfolioDao;
    }

    /** Builds a holistic portfolio valuation for one brokerage account. */
    public PortfolioResponse getPortfolio(long accountId) {
        Optional<BigDecimal> cashOptional = portfolioDao.cashBalance(accountId);
        if (cashOptional.isEmpty()) {
            throw new AccountNotFoundException("Account not found");
        }
        BigDecimal cash = cashOptional.get();

        List<Holding> holdings = portfolioDao.holdings(accountId);
        BigDecimal holdingsValue = BigDecimal.ZERO;
        for (Holding holding : holdings) {
            holdingsValue = holdingsValue.add(holding.currentValue());
        }

        return new PortfolioResponse(accountId, cash, holdings, holdingsValue, cash.add(holdingsValue));
    }
}
