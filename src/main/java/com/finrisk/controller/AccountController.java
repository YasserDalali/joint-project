package com.finrisk.controller;

import com.finrisk.dto.request.AccountCreateRequest;
import com.finrisk.dto.request.CashMovementRequest;
import com.finrisk.dto.response.AccountResponse;
import com.finrisk.dto.response.Page;
import com.finrisk.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HTTP endpoints for opening accounts, adjusting cash, and listing accounts per user. */
@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService accountService;

    /** Injects {@link AccountService} handling orchestration for this controller. */
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** Creates a brokerage account tied to an existing user identifier. */
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountCreateRequest req) {
        return accountService.createAccount(req);
    }

    /** Retrieves account metadata including balances by numeric id. */
    @GetMapping("/accounts/{id}")
    public AccountResponse get(@PathVariable long id) {
        return accountService.getAccount(id);
    }

    /** Credits cash into an account wallet via POST body amount. */
    @PostMapping("/accounts/{id}/deposit")
    public AccountResponse deposit(@PathVariable long id, @Valid @RequestBody CashMovementRequest req) {
        return accountService.deposit(id, req);
    }

    /** Debits cash when sufficient funds exist according to service validation. */
    @PostMapping("/accounts/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable long id, @Valid @RequestBody CashMovementRequest req) {
        return accountService.withdraw(id, req);
    }

    /** Paginates accounts owned by the supplied {@code userId} path variable. */
    @GetMapping("/users/{userId}/accounts")
    public Page<AccountResponse> listByUser(
            @PathVariable long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort) {
        return accountService.listForUser(userId, page, size, sort == null ? List.of() : sort);
    }
}
