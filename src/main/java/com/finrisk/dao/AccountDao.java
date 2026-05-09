package com.finrisk.dao;

import com.finrisk.dto.response.Page;
import com.finrisk.model.Account;

import java.math.BigDecimal;
import java.util.List;

public interface AccountDao extends GenericDao<Account, Long> {

    Page<Account> pageByUserId(Long userId, int page, int size, List<String> sortSpecs);

    void updateCashBalance(Long id, BigDecimal newBalance);
}
