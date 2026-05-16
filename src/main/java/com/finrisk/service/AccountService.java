package com.finrisk.service;

import com.finrisk.dao.AccountDao;
import com.finrisk.dao.UserDao;
import com.finrisk.dto.request.AccountCreateRequest;
import com.finrisk.dto.request.CashMovementRequest;
import com.finrisk.dto.response.AccountResponse;
import com.finrisk.dto.response.Page;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.exception.InsufficientBalanceException;
import com.finrisk.exception.UserNotFoundException;
import com.finrisk.mapper.AccountMapper;
import com.finrisk.model.Account;
import com.finrisk.model.User;
import com.finrisk.util.SqlSort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Coordinates account lifecycle operations combining {@link AccountDao} and {@link UserDao} validation. */
@Service
public class AccountService {

    private final AccountDao accountDao;
    private final UserDao userDao;

    /** Wires persistence collaborators required for account workflows. */
    public AccountService(AccountDao accountDao, UserDao userDao) {
        this.accountDao = accountDao;
        this.userDao = userDao;
    }

    /** Opens a brokerage account after confirming the owner {@link User} exists. */
    public AccountResponse createAccount(AccountCreateRequest req) {
        User owner = userDao.findById(req.userId());
        if (owner == null) {
            throw new UserNotFoundException("Owning user not found");
        }
        Account account = AccountMapper.toNewAccount(req);
        Account saved = accountDao.save(account);
        return AccountMapper.toResponse(saved);
    }

    /** Fetches a single account by id throwing {@link AccountNotFoundException} if missing. */
    public AccountResponse getAccount(long id) {
        Account account = accountDao.findById(id);
        if (account == null) {
            throw new AccountNotFoundException("Account not found");
        }
        return AccountMapper.toResponse(account);
    }

    /** Lists accounts for a user with pagination/sorting mirroring SQL constraints. */
    public Page<AccountResponse> listForUser(long userId, int page, int size, List<String> sort) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<Account> accountPage =
                accountDao.pageByUserId(userId, safePage, safeSize, SqlSort.normalizeSortParams(sort));

        List<AccountResponse> responses = new ArrayList<>();
        for (Account account : accountPage.content()) {
            responses.add(AccountMapper.toResponse(account));
        }

        return new Page<>(
                accountPage.page(),
                accountPage.size(),
                accountPage.totalElements(),
                accountPage.totalPages(),
                accountPage.first(),
                accountPage.last(),
                responses);
    }

    /** Credits cash to an account balance atomically at the DAO layer. */
    public AccountResponse deposit(long accountId, CashMovementRequest req) {
        Account account = requireAccount(accountId);
        BigDecimal nextBalance = account.cashBalance().add(req.amount());
        accountDao.updateCashBalance(accountId, nextBalance);
        return AccountMapper.toResponse(account.withCashBalance(nextBalance));
    }

    /** Debits cash after verifying sufficient funds are available locally. */
    public AccountResponse withdraw(long accountId, CashMovementRequest req) {
        Account account = requireAccount(accountId);
        if (account.cashBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient cash balance");
        }
        BigDecimal nextBalance = account.cashBalance().subtract(req.amount());
        accountDao.updateCashBalance(accountId, nextBalance);
        return AccountMapper.toResponse(account.withCashBalance(nextBalance));
    }

    /** Shared guard ensuring subsequent operations target a real {@link Account}. */
    private Account requireAccount(long id) {
        Account account = accountDao.findById(id);
        if (account == null) {
            throw new AccountNotFoundException("Account not found");
        }
        return account;
    }
}
