package com.finrisk.dao;

import com.finrisk.dto.response.Holding;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Read-model DAO projecting portfolio holdings plus cash for aggregate endpoints. */
public interface PortfolioDao {

    /** Reads the current cash balance column for an account if present. */
    Optional<BigDecimal> cashBalance(long accountId);

    /** Lists joined holdings rows describing quantities and live valuations. */
    List<Holding> holdings(long accountId);
}
