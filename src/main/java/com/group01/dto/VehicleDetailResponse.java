package com.group01.dto;

import java.math.BigDecimal;

/** Full vehicle page data. {@code finalDailyPrice} = dailyPrice x (1 - discountRate). */
public record VehicleDetailResponse(
        String vehicleCode,
        String name,
        CategoryResponse category,
        BranchResponse branch,
        String licensePlate,
        String imageUrl,
        String shortDescription,
        String detailedDescription,
        BigDecimal dailyPrice,
        BigDecimal discountRate,
        BigDecimal finalDailyPrice,
        String status,
        int viewCount,
        int rentalCount
) {
}
