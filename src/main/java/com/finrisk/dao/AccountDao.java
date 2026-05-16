package com.finrisk.dao;

import com.finrisk.dto.response.Page;
import com.finrisk.model.Account;

import java.math.BigDecimal;
import java.util.List;

/** Persists brokerage accounts and exposes paging helpers per owning user. */
public interface AccountDao extends GenericDao<Account, Long> {

    /** Lists accounts belonging to one user with SQL-safe sorting. */
    Page<Account> pageByUserId(Long userId, int page, int size, List<String> sortSpecs);

    /** Writes a new cash balance snapshot directly at the persistence layer. */
    void updateCashBalance(Long id, BigDecimal newBalance);
}
