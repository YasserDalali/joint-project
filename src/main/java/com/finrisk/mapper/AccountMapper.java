package com.finrisk.mapper;

import com.finrisk.dto.request.AccountCreateRequest;
import com.finrisk.dto.response.AccountResponse;
import com.finrisk.model.Account;

/** Bridges {@link AccountCreateRequest}/{@link AccountResponse} DTOs with {@link Account} domain records. */
public final class AccountMapper {

    /** Declares this mapper as static-only utilities. */
    private AccountMapper() {}

    /** Creates an unsaved {@link Account} using signup parameters plus initial deposit. */
    public static Account toNewAccount(AccountCreateRequest req) {
        return new Account(null, req.userId(), req.accountName(), req.initialDeposit(), null);
    }

    /** Converts a persisted {@link Account} into JSON-ready structures including USD defaults. */
    public static AccountResponse toResponse(Account a) {
        return new AccountResponse(a.id(), a.userId(), a.accountName(), a.cashBalance(), a.createdAt());
    }
}
