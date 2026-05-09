package com.finrisk.mapper;

import com.finrisk.dto.request.AccountCreateRequest;
import com.finrisk.dto.response.AccountResponse;
import com.finrisk.model.Account;

public final class AccountMapper {

    private AccountMapper() {}

    public static Account toNewAccount(AccountCreateRequest req) {
        return new Account(null, req.userId(), req.accountName(), req.initialDeposit(), null);
    }

    public static AccountResponse toResponse(Account a) {
        return new AccountResponse(a.id(), a.userId(), a.accountName(), a.cashBalance(), a.createdAt());
    }
}
