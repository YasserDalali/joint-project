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
import java.util.List;

@Service
public class AccountService {

    private final AccountDao accountDao;
    private final UserDao userDao;

    public AccountService(AccountDao accountDao, UserDao userDao) {
        this.accountDao = accountDao;
        this.userDao = userDao;
    }

    public AccountResponse createAccount(AccountCreateRequest req) {
        User owner = userDao.findById(req.userId());
        if (owner == null) {
            throw new UserNotFoundException("Owning user not found");
        }
        Account a = AccountMapper.toNewAccount(req);
        Account saved = accountDao.save(a);
        return AccountMapper.toResponse(saved);
    }

    public AccountResponse getAccount(long id) {
        Account a = accountDao.findById(id);
        if (a == null) {
            throw new AccountNotFoundException("Account not found");
        }
        return AccountMapper.toResponse(a);
    }

    public Page<AccountResponse> listForUser(long userId, int page, int size, List<String> sort) {
        User u = userDao.findById(userId);
        if (u == null) {
            throw new UserNotFoundException("User not found");
        }
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Page<Account> p =
                accountDao.pageByUserId(userId, safePage, safeSize, SqlSort.normalizeSortParams(sort));
        return new Page<>(
                p.page(),
                p.size(),
                p.totalElements(),
                p.totalPages(),
                p.first(),
                p.last(),
                p.content().stream().map(AccountMapper::toResponse).toList());
    }

    public AccountResponse deposit(long accountId, CashMovementRequest req) {
        Account a = requireAccount(accountId);
        BigDecimal next = a.cashBalance().add(req.amount());
        accountDao.updateCashBalance(accountId, next);
        return AccountMapper.toResponse(a.withCashBalance(next));
    }

    public AccountResponse withdraw(long accountId, CashMovementRequest req) {
        Account a = requireAccount(accountId);
        if (a.cashBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient cash balance");
        }
        BigDecimal next = a.cashBalance().subtract(req.amount());
        accountDao.updateCashBalance(accountId, next);
        return AccountMapper.toResponse(a.withCashBalance(next));
    }

    private Account requireAccount(long id) {
        Account a = accountDao.findById(id);
        if (a == null) {
            throw new AccountNotFoundException("Account not found");
        }
        return a;
    }
}
