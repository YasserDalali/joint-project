package com.finrisk.controller;

import com.finrisk.dao.TransactionPageQuery;
import com.finrisk.dto.request.TradeRequest;
import com.finrisk.dto.response.Page;
import com.finrisk.dto.response.TransactionResponse;
import com.finrisk.model.TransactionType;
import com.finrisk.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** Handles trade submissions and paginated ledger reads for brokerage accounts. */
@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private final TransactionService transactionService;

    /** Provides {@link TransactionService} orchestrating stored procedure execution. */
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /** Accepts JSON describing a buy trade and returns confirmation payload. */
    @PostMapping("/transactions/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse buy(@Valid @RequestBody TradeRequest req) {
        return transactionService.buy(req);
    }

    /** Mirrors {@link #buy(TradeRequest)} for sells using identical validation pipeline. */
    @PostMapping("/transactions/sell")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse sell(@Valid @RequestBody TradeRequest req) {
        return transactionService.sell(req);
    }

    /** Lists historical trades for an account with optional filters encoded as query params. */
    @GetMapping("/accounts/{accountId}/transactions")
    public Page<TransactionResponse> list(
            @PathVariable long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort) {
        return transactionService.listForAccount(
                new TransactionPageQuery(
                        accountId,
                        type,
                        assetId,
                        from,
                        to,
                        page,
                        size,
                        sort == null ? List.of() : sort));
    }
}
