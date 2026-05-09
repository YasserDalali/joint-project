package com.finrisk.dao;

import com.finrisk.dto.response.Holding;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PortfolioDao {

    Optional<BigDecimal> cashBalance(long accountId);

    List<Holding> holdings(long accountId);
}
