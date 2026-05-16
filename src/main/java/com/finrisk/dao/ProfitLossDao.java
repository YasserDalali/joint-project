package com.finrisk.dao;

import com.finrisk.dto.response.HoldingProfitLoss;

import java.util.List;

/** DAO computing profit-and-loss projections per holding for analytics endpoints. */
public interface ProfitLossDao {

    /** Builds profit rows summarizing cost basis versus market value for each position. */
    List<HoldingProfitLoss> holdings(long accountId);
}
