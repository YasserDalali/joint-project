package com.finrisk.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SectorAllocation(
        String sector,
        double weight,
        BigDecimal value,
        List<String> symbols) {}
