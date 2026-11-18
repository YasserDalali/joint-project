package com.finrisk.dto.response;

public record DownsideResponse(
        long accountId,
        Double downsideDeviation,
        Double sortinoRatio,
        Double downsideBeta,
        Double worstDay,
        String benchmark) {}
