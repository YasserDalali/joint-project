package com.finrisk.dao;

import com.finrisk.model.TransactionType;

import java.time.LocalDateTime;
import java.util.List;

/** Parameters for {@link TransactionDao#pageForAccount(TransactionPageQuery)} (keeps arity within Sonar limits). */
public record TransactionPageQuery(
        long accountId,
        TransactionType type,
        Long assetId,
        LocalDateTime fromInclusive,
        LocalDateTime toExclusive,
        int page,
        int size,
        List<String> sortSpecs) {}
