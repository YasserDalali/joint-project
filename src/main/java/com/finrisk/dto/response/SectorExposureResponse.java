package com.finrisk.dto.response;

import java.util.List;

public record SectorExposureResponse(long accountId, List<SectorAllocation> sectors) {}
