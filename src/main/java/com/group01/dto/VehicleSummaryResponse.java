package com.group01.dto;

import java.math.BigDecimal;

/** A vehicle overview; intentionally excludes entity relations. */
public record VehicleSummaryResponse(
    String vehicleCode,
    String name,
    Long categoryId,
    Long branchId,
    BigDecimal dailyPrice,
    String status
) {}
