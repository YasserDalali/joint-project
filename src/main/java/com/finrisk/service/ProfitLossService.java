package com.finrisk.service;

import com.finrisk.dao.PortfolioDao;
import com.finrisk.dao.ProfitLossDao;
import com.finrisk.dto.response.HoldingProfitLoss;
import com.finrisk.dto.response.ProfitLossResponse;
import com.finrisk.exception.AccountNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** Surfaces profit-and-loss analytics derived from DAO-side aggregations for an account. */
@Service
public class ProfitLossService {

    private final PortfolioDao portfolioDao;
    private final ProfitLossDao profitLossDao;

    /** Collects dependencies capable of confirming accounts and computing per-holding P&amp;L rows. */
    public ProfitLossService(PortfolioDao portfolioDao, ProfitLossDao profitLossDao) {
        this.portfolioDao = portfolioDao;
        this.profitLossDao = profitLossDao;
    }

    /** Returns profit breakdown rows plus summed portfolio P&amp;L for REST consumers. */
    public ProfitLossResponse getProfitLoss(long accountId) {
        if (portfolioDao.cashBalance(accountId).isEmpty()) {
            throw new AccountNotFoundException("Account not found");
        }
        List<HoldingProfitLoss> rows = profitLossDao.holdings(accountId);

        BigDecimal total = BigDecimal.ZERO;
        for (HoldingProfitLoss row : rows) {
            total = total.add(row.profitLoss());
        }

        return new ProfitLossResponse(accountId, rows, total);
    }
}
