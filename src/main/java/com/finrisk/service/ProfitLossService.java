package com.finrisk.service;

import com.finrisk.dao.PortfolioDao;
import com.finrisk.dao.ProfitLossDao;
import com.finrisk.dto.response.HoldingProfitLoss;
import com.finrisk.dto.response.ProfitLossResponse;
import com.finrisk.exception.AccountNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProfitLossService {

    private final PortfolioDao portfolioDao;
    private final ProfitLossDao profitLossDao;

    public ProfitLossService(PortfolioDao portfolioDao, ProfitLossDao profitLossDao) {
        this.portfolioDao = portfolioDao;
        this.profitLossDao = profitLossDao;
    }

    public ProfitLossResponse getProfitLoss(long accountId) {
        if (portfolioDao.cashBalance(accountId).isEmpty()) {
            throw new AccountNotFoundException("Account not found");
        }
        List<HoldingProfitLoss> rows = profitLossDao.holdings(accountId);
        BigDecimal total =
                rows.stream().map(HoldingProfitLoss::profitLoss).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProfitLossResponse(accountId, rows, total);
    }
}
