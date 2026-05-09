package com.finrisk.dao;

import com.finrisk.dto.response.HoldingProfitLoss;

import java.util.List;

public interface ProfitLossDao {

    List<HoldingProfitLoss> holdings(long accountId);
}
