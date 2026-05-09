package com.finrisk.service;

import com.finrisk.dao.PortfolioDao;
import com.finrisk.dto.response.Holding;
import com.finrisk.dto.response.PortfolioResponse;
import com.finrisk.exception.AccountNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortfolioService {

    private final PortfolioDao portfolioDao;

    public PortfolioService(PortfolioDao portfolioDao) {
        this.portfolioDao = portfolioDao;
    }

    public PortfolioResponse getPortfolio(long accountId) {
        BigDecimal cash =
                portfolioDao
                        .cashBalance(accountId)
                        .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        List<Holding> holdings = portfolioDao.holdings(accountId);
        BigDecimal holdingsValue =
                holdings.stream().map(Holding::currentValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PortfolioResponse(accountId, cash, holdings, holdingsValue, cash.add(holdingsValue));
    }
}
