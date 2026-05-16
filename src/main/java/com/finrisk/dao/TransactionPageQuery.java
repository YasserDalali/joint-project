package com.finrisk.dao;

import com.finrisk.model.TransactionType;

import java.time.LocalDateTime;
import java.util.List;

/** Immutable bundle of filters and paging knobs passed into {@link TransactionDao#pageForAccount(TransactionPageQuery)}. */
public record TransactionPageQuery(
        long accountId,
        TransactionType type,
        Long assetId,
        LocalDateTime fromInclusive,
        LocalDateTime toExclusive,
        int page,
        int size,
        List<String> sortSpecs) {}
